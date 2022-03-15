package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Logging
import com.onionchat.common.MessageStatus
import com.onionchat.common.SettingsManager
import com.onionchat.connector.http.OnionClient
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.userstore.User
import java.util.concurrent.Future

class ForwardMessageTask(val encryptedMessage: EncryptedMessage) : OnionTask<ForwardMessageTask.ForwardMessageResult>() {


    class ForwardMessageResult(val forwardedToUsers: List<User> = listOf(), status: Status, exception: Exception? = null) :
        OnionTask.Result(status, exception) {

    }

    override fun run(): ForwardMessageResult {

        Logging.d(TAG, "run [+] forward message $encryptedMessage state=(${MessageStatus.dumpState(encryptedMessage.messageStatus)})")


        // 0. check if forwarding is enabled
        context?.let {
            val doForward = SettingsManager.getBooleanSetting(it.getString(R.string.key_enable_message_forwarding), it)
        }

        // 0. check if already forwarded


        val forwardedToUsers = mutableListOf<User>()
        if (MessageStatus.hasFlag(encryptedMessage.messageStatus, MessageStatus.FORWARDED)) {
            throw IllegalStateException("Message already sent $encryptedMessage")
        }

        // 1. get all users
        val futures = HashMap<User, Future<OnionClient.MessageSentResult>>()
        UserManager.getAllUsers().get().forEach {
            if (it.id != UserManager.myId) { // dont forward to myself
                // check is blocked
                futures[it] = OnionClient.postmessage(EncryptedMessage.toJson(encryptedMessage), it.id)
            }
        }
        futures.forEach {
            val status = it.value.get()
            if (status == OnionClient.MessageSentResult.SENT) {

                forwardedToUsers.add(it.key)
            } else {
                Logging.d(TAG, "sent <${status}, ${status}>")
            }
        }
        Logging.d(TAG, "forwardedToUsers <${forwardedToUsers.size}>")

        if (forwardedToUsers.isNotEmpty()) {
            // update state
            encryptedMessage.messageStatus = MessageStatus.addFlag(encryptedMessage.messageStatus, MessageStatus.FORWARDED)
            MessageManager.updateEncryptedMessage(encryptedMessage)
            Logging.d(TAG, "run [+] successfully forwarded message $encryptedMessage (${forwardedToUsers.size}) state=<${MessageStatus.dumpState(encryptedMessage.messageStatus)}>")
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