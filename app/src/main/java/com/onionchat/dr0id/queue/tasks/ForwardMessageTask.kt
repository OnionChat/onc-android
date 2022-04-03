package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Logging
import com.onionchat.common.MessageStatus
import com.onionchat.common.SettingsManager
import com.onionchat.connector.http.OnionClient
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.BroadcastManager
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.messagestore.MessageForwardInfo
import com.onionchat.localstorage.userstore.User
import java.util.*
import java.util.concurrent.Future
import kotlin.collections.HashMap

class ForwardMessageTask(val encryptedMessage: EncryptedMessage) : OnionTask<ForwardMessageTask.ForwardMessageResult>() {

    init {
        Logging.d(TAG, "init [+] created forward task <id=${super.id}, messageId=${encryptedMessage.messageId}>")
    }

    class ForwardMessageResult(val forwardedToUsers: List<MessageForwardInfo> = listOf(), status: Status, exception: Exception? = null) :
        OnionTask.Result(status, exception) {

    }

    override fun run(): ForwardMessageResult {

        Logging.d(TAG, "run [+] forward message $encryptedMessage state=(${MessageStatus.dumpState(encryptedMessage.messageStatus)})")


        // 0. check if forwarding is enabled
        context?.let {
            val doForward = SettingsManager.getBooleanSetting(it.getString(R.string.key_enable_message_forwarding), it)
        }

        // 0. check if already forwarded
        val forwardedToUsers = mutableListOf<MessageForwardInfo>()
        Logging.d(TAG, "run [-] message has flags ${MessageStatus.dumpState(encryptedMessage.messageStatus)}")
        if (MessageStatus.hasFlag(encryptedMessage.messageStatus, MessageStatus.FORWARDED)) { //  && !encryptedMessage.isBroadcast()
            throw IllegalStateException("Message already sent $encryptedMessage")
        }

        // 1. get target information and information about what users we already have sent this message
        var wasForwardedToAnyUser = false
        val details = MessageManager.getEncryptedMessageDetails(encryptedMessage.messageId).get()
        if (details == null) {
            Logging.e(TAG, "run [+] seems like we have an non persistent message")
        } else {
            if (details.messageForwardInformation.isNotEmpty()) {
                wasForwardedToAnyUser = true
                forwardedToUsers.addAll(details.messageForwardInformation)
            }
        }

        val targetUsers = ArrayList<User>()
        val broadcast = encryptedMessage.getBroadcast()
        if (broadcast != null) {
            targetUsers.addAll(BroadcastManager.getBroadcastUsers(broadcast).get())
        } else {
            val targetUser = UserManager.getUserByHashedId(encryptedMessage.hashedTo).get()
            if (targetUser == null) {
                Logging.e(TAG, "run [+] seems like we don't know the target user....")
            } else {
                targetUsers.add(targetUser)
            }
        }


        // 2. try forward to all users
        val futures = HashMap<User, Future<OnionClient.MessageSentResult>>()
        val users = UserManager.getAllUsers().get()
        if (!wasForwardedToAnyUser) {
            users.forEach {
                if (it.id != UserManager.myId) { // dont forward to myself
                    // todo check is blocked
                    var forward = true
                    // check if we have already forwarded this message
                    details?.messageForwardInformation?.forEach { forwardInfo ->
                        if (it.getHashedId() == forwardInfo.userId) {
                            forward = false
                        }
                    }
                    if (forward) {
                        futures[it] = OnionClient.postmessage(EncryptedMessage.toJson(encryptedMessage), it.id)
                    }
                }
            }
        } else if (targetUsers.isNotEmpty()) {
            Logging.d(TAG, "run [+] message was already forwarded to someone... so let's concetrate on the target users itself...")
            targetUsers.forEach {
                var forward = true
                details?.messageForwardInformation?.forEach { forwardInfo ->
                    if (it.getHashedId() == forwardInfo.userId) {
                        forward = false
                    }
                }
                if (forward) {
                    futures[it] = OnionClient.postmessage(EncryptedMessage.toJson(encryptedMessage), it.id)
                }
            }
        } else {
            Logging.e(TAG, "run [-] Illegal state !! was forwarded to any user and target user is null !! there is a bug dude...")
            return ForwardMessageResult(status = Status.FAILURE)
        }

        val newForwardedToUsers = ArrayList<MessageForwardInfo>()
        futures.forEach {
            val status = it.value.get()
            if (status == OnionClient.MessageSentResult.SENT) {

                Logging.d(TAG, "run [+] we forwarded message ${encryptedMessage.messageId} to user ${it.key.getHashedId()}")

                val info = MessageForwardInfo(
                    UUID.randomUUID().toString(),
                    encryptedMessage.messageId,
                    it.key.getHashedId(),
                    System.currentTimeMillis()
                )
                newForwardedToUsers.add(
                    info
                )
                forwardedToUsers.add(
                    info
                )
            } else {
                Logging.d(TAG, "sent <${status}, ${status}>")
            }
        }
        Logging.d(TAG, "forwardedToUsers <${forwardedToUsers.size}>")

        // 4. check results
        if (newForwardedToUsers.isNotEmpty()) {

            if (details != null) { // its a persistent stored message
                MessageManager.insertForwardInformation(newForwardedToUsers) // fire forget !?
                var finallyForwarded = true
                targetUsers.forEach { targetUser ->
                    var forwarded = false
                    forwardedToUsers.forEach { forwardedUser ->
                        if (targetUser.getHashedId() == forwardedUser.userId) {
                            forwarded = true
                        }
                    }
                    if (finallyForwarded) { // if at somepoint it was set to false we're not allowed to overwrite
                        finallyForwarded = forwarded
                    }
                }
                if (finallyForwarded) {
                    Logging.d(TAG, "run [+] we finally forwarded message ${encryptedMessage.messageId} to <${forwardedToUsers.size}> users")
                    // update state
                    val latest = MessageManager.getMessageById(encryptedMessage.messageId).get()!! // todo remove message status integer
                    latest.messageStatus = MessageStatus.addFlag(encryptedMessage.messageStatus, MessageStatus.FORWARDED)
                    MessageManager.updateEncryptedMessage(latest)
                }
            }

            Logging.d(
                TAG,
                "run [+] successfully forwarded message $encryptedMessage (${forwardedToUsers}) state=<${MessageStatus.dumpState(encryptedMessage.messageStatus)}>"
            )
            return ForwardMessageResult(forwardedToUsers, Status.SUCCESS)
        }
        return ForwardMessageResult(status = Status.FAILURE)
    }

    override fun onUnhandledException(exception: Exception): ForwardMessageResult {
        return ForwardMessageResult(status = Status.FAILURE, exception = exception)
    }

    companion object {
        val TAG = "ForwardMessageTask"
    }
}