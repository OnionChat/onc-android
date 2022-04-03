package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.*
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.*
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.MessageProcessor
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.dr0id.messaging.keyexchange.NegotiateSymKeyMessage
import com.onionchat.dr0id.messaging.keyexchange.ResponsePubMessage
import com.onionchat.dr0id.messaging.messages.*
import com.onionchat.dr0id.queue.OnionFuture
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.userstore.Broadcast
import com.onionchat.localstorage.userstore.User
import java.util.*

class ReceiveMessageTask(val rawMessageString: String) : OnionTask<ReceiveMessageTask.ReceiveMessageResult>() {


    class ReceiveMessageResult(
        status: Status,
        val message: IMessage? = null,
        val encryptedMessage: EncryptedMessage? = null,
        val broadcast: Broadcast? = null,
        exception: java.lang.Exception? = null
    ) : OnionTask.Result(status, exception) {

    }

    override fun run(): ReceiveMessageResult {

        // 0. decode encrypted message
        val encryptedMessage = EncryptedMessage.fromJson(rawMessageString)


        // 1. check if message has to be stored... and store it
        if (MessageManager.getMessageById(encryptedMessage.messageId).get() != null || encryptedMessage.hashedFrom == UserManager.getMyHashedId()) {
            Logging.d(TAG, "run [-] message already exists <${encryptedMessage.messageId}>")
            return ReceiveMessageResult(Status.FAILURE) // todo really?
        }
        encryptedMessage.messageStatus = MessageStatus.setFlags(MessageStatus.RECEIVED)
        MessageManager.storeEncryptedMessage(encryptedMessage)
        Logging.d(TAG, "run [-] message has flags ${MessageStatus.dumpState(encryptedMessage.messageStatus)}")

        // 2. check if broadcast message
        val broadcast: Broadcast? = Broadcast.createFromPayload(encryptedMessage.extra)
        if (broadcast != null) {
            val context = context
            if (context == null) {
                Logging.d(TAG, "run [-] unable to process broadcast... context is null")
                return ReceiveMessageResult(Status.FAILURE) // todo really?
            }
            val storedBroadcast = BroadcastManager.getBroadcastById(broadcast.id).get()
            if (storedBroadcast == null) {
                Logging.d(TAG, "run [+] new broadcast found")
                val allowed = SettingsManager.getBooleanSetting(context.getString(R.string.key_allow_broadcast_adding), context)
                if (allowed) {
                    if (!BroadcastManager.addBroadcast(broadcast).get()) {
                        Logging.e(TAG, "run [-] error while adding broadcast")
                        return ReceiveMessageResult(Status.FAILURE)
                    }
                    val default_add_all_users = SettingsManager.getBooleanSetting(context.getString(R.string.key_default_add_all_users), context)
                    if (default_add_all_users) {
                        BroadcastManager.addUsersToBroadcast(broadcast, UserManager.getAllUsers().get())
                    }
                } else {
                    Logging.d(TAG, "run [+] broadcast auto add disabled")
                }
            }
        }

        // 3. check if feed
        if (encryptedMessage.hashedTo == Conversation.DEFAULT_FEED_ID) {

        }

        // 4. check if message is for me
        val myId = IDGenerator.toHashedId(UserManager.myId!!);
        if (encryptedMessage.hashedTo != myId && broadcast == null && encryptedMessage.hashedTo != Conversation.DEFAULT_FEED_ID) {
            Logging.d(TAG, "run [-] i'm not the reciever.. forward? <${encryptedMessage.hashedTo}> (my id is <$myId>))")
            // todo forward
            return ReceiveMessageResult(Status.FAILURE)
        }

        // check if we have to forward it
        // 5. check if forward message
        var forwardTask: OnionFuture<ForwardMessageTask.ForwardMessageResult>? = null
        if (broadcast != null) { // todo add more message types
            // todo forward ?
            forwardTask = enqueueSubtask(ForwardMessageTask(encryptedMessage))
        }

        // 3. check if blocked

        // 4. try to unpack message

        //// my crypto
        val myPrivate = Crypto.getMyKey()

        var sender: User? = null
        UserManager.getAllUsers().get().forEach {
            if (it.getHashedId() == encryptedMessage.hashedFrom) {
                sender = it
            }
        }
        if (sender == null) { // todo shall we forward?
            // User is not in our database
            Logging.d(TAG, "run [-] unknown user <${encryptedMessage.hashedFrom}>")
        }

        // todo check if it's a blocked user
        val pubAlias = sender?.certId //broadcast?.pub_alias

        val sourcePub = pubAlias?.let { Crypto.getPublicKey(it) }
        val decryptedMessage = try {
            MessageProcessor.unpack(encryptedMessage, sourcePub, myPrivate)
        } catch (unknownKeyException: SymmetricMessage.UnknownKeyException) {
            if (sender != null) {
                Logging.e(TAG, "run [-] unable to decrypt message of user <${sender}> [-] enqueue request sym message", unknownKeyException)
                enqueueFollowUpTask(
                    SendMessageTask(
                        RequestSymKeyMessage(
                            alias = unknownKeyException.alias,
                            hashedFrom = UserManager.getMyHashedId(),
                            hashedTo = encryptedMessage.hashedFrom
                        ), Conversation(user = sender)
                    )
                )
                null
            } else {
                Logging.e(TAG, "run [-] unable to decrypt message from unknown user <${encryptedMessage.hashedFrom}>", unknownKeyException)

                null
            }

        }
        if (decryptedMessage == null) {
            Logging.e(TAG, "run [-] unable to decrypt message <$decryptedMessage>")
            return ReceiveMessageResult(Status.FAILURE)
        }

        if (decryptedMessage is NegotiateSymKeyMessage) {
            enqueueFollowUpTask(HandleNegotiateSymKeyTask(decryptedMessage))
        } else if (decryptedMessage is ResponsePubMessage) {
            // todo ask user?
            Logging.d(TAG, "Added user ${decryptedMessage.addUserPayload}")
            UserManager.addUser(decryptedMessage.addUserPayload)
        } else if (decryptedMessage is MessageReadMessage) {
            Logging.d(TAG, "run [+] update message status <${decryptedMessage.messageSignature}>")
            MessageManager.setMessageRead(decryptedMessage)
        } else if (decryptedMessage is AttachmentMessage) {
            Logging.d(TAG, "run [+] trying to download attachment <${decryptedMessage.getAttachment()}>")
            DownloadManager.enqueueDownload(sourcePub, decryptedMessage.getAttachment(), true)
        } else if (decryptedMessage is RequestContactDetailsMessage) {
            Logging.d(TAG, "run [+] initiate ProvideContactDetailsTask for user $sender")
            if (sender == null) {
                Logging.e(TAG, "run [-] unable to start ProvideContactDetailsTask... unknown user")
                return ReceiveMessageResult(Status.FAILURE)
            }
            enqueueFollowUpTask(ProvideContactDetailsTask(sender!!))
        } else if (decryptedMessage is ProvideContactDetailsMessage) {
            if (decryptedMessage.hashedFrom != IDGenerator.toHashedId(decryptedMessage.contactDetails.userid)) {
                Logging.e(
                    TAG,
                    "run [-] unable to store ContactDetails... initiator and details don't match. We cannot trust this! ${decryptedMessage.contactDetails.id}"
                )
                return ReceiveMessageResult(Status.FAILURE)
            }
            Logging.d(TAG, "run [+] addContactDetails ${decryptedMessage.contactDetails}")

            UserManager.addContactDetails(decryptedMessage.contactDetails)
        } else if (decryptedMessage is RequestSymKeyMessage) {
            if(sender == null) {
                Logging.e(TAG, "run [-] received RequestSymKey from unkown user <${decryptedMessage.hashedFrom}>")
                return ReceiveMessageResult(Status.FAILURE)
            }
            val requestedAlias = decryptedMessage.alias
            Logging.d(TAG, "run [+] sym key <${requestedAlias}> requested by user $sender")
            // feed key
            UserManager.getFeedKey(requestedAlias).get()?.let { feedKey ->
                val asymEncryptedKey = feedKey.encryptedKey
                val decryptedKey = Crypto.decryptAsym(Crypto.getMyKey(), asymEncryptedKey)
                if(decryptedKey == null) {
                    Logging.e(TAG, "run [-] unable to get feed key... abort")
                    return ReceiveMessageResult(Status.FAILURE)
                }
                val targetPub = Crypto.getPublicKey(sender!!.certId)
                if(targetPub == null) {
                    Logging.e(TAG, "run [-] unable to get public key for user <${sender}>")
                    return ReceiveMessageResult(Status.FAILURE)
                }
                val targetAsymEncryptedKey = Crypto.encryptAsym(targetPub, decryptedKey)
                Logging.d(TAG, "run [+] finally encryted <${requestedAlias}> for user $sender [+] sending the payload now")

                enqueueFollowUpTask(
                    SendMessageTask(
                        ProvideSymKeyMessage(
                            symKey = SymKeyPayload(UUID.randomUUID().toString(), feedKey.alias,targetAsymEncryptedKey, System.currentTimeMillis()),
                            hashedFrom = UserManager.getMyHashedId(),
                            hashedTo = encryptedMessage.hashedFrom
                        ), Conversation(user = sender)
                    )
                )
            } ?: run {
                Logging.e(TAG, "run [-] sym key not found <${requestedAlias}>")
            }

            // todo add group stuff !=

            // todo add key negotioation stuff !?
        } else if(decryptedMessage is ProvideSymKeyMessage) {
            Logging.d(TAG, "run [+] received ${decryptedMessage} [+] adding key to keystore")
            val encryptedKey = decryptedMessage.symKey.encryptedKey
            val decryptedKey = Crypto.decryptAsym(Crypto.getMyKey(), encryptedKey)
            if(decryptedKey == null) {
                Logging.e(TAG, "run [-] unable to decrypt provided key <${decryptedMessage.symKey}>")
                return ReceiveMessageResult(Status.FAILURE)
            }
            Crypto.storeSymmetricKey(decryptedMessage.symKey.alias, decryptedKey)
        }

        forwardTask?.get().let { // just for information... we may don't need the result here
            if (it?.status == Status.SUCCESS) {
                Logging.d(TAG, "run [+] successfully forwarded message ${it?.status}")
            }
        }


        // 6. notify listeners or return result
        return ReceiveMessageResult(Status.SUCCESS, message = decryptedMessage, encryptedMessage = encryptedMessage, broadcast = broadcast)
    }

    override fun onUnhandledException(exception: Exception): ReceiveMessageResult {
        return ReceiveMessageResult(Status.FAILURE, exception = exception)
    }

    companion object {
        const val TAG = "ReceiveMessageTask"
    }
}