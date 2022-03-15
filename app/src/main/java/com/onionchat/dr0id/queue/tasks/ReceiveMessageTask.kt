package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.*
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.MessageProcessor
import com.onionchat.dr0id.messaging.keyexchange.NegotiateSymKeyMessage
import com.onionchat.dr0id.messaging.messages.MessageReadMessage
import com.onionchat.dr0id.queue.OnionFuture
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.userstore.User

class ReceiveMessageTask(val rawMessageString: String) : OnionTask<ReceiveMessageTask.ReceiveMessageResult>() {


    class ReceiveMessageResult(status: Status, val message: IMessage? = null, exception: java.lang.Exception? = null) : OnionTask.Result(status, exception) {

    }

    override fun run(): ReceiveMessageResult {

        // 0. decode encrypted message
        val encryptedMessage = EncryptedMessage.fromJson(rawMessageString)


        // 1. check if message has to be stored... and store it
        encryptedMessage.messageStatus = MessageStatus.setFlags(MessageStatus.RECEIVED)
        MessageManager.storeEncryptedMessage(encryptedMessage)

        // 2. check if message is for me
        val myId = IDGenerator.toHashedId(UserManager.myId!!);
        if (!encryptedMessage.hashedTo.equals(myId)) {
            Logging.d(TAG, "run [-] i'm not the reciever.. forward? <${encryptedMessage.hashedTo}> (my id is <$myId>))")
            // todo forward
            return ReceiveMessageResult(Status.SUCCESS)
        }

        // check if we have to forward it
        // 5. check if forward message
        var forwardTask: OnionFuture<ForwardMessageTask.ForwardMessageResult>? = null
        if (encryptedMessage.type == MessageTypes.BROADCAST_TEXT_MESSAGE.ordinal) { // todo add more message types
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

        val sourcePub = sender?.certId?.let { Crypto.getPublicKey(it) }
        val decryptedMessage = MessageProcessor.unpack(encryptedMessage, sourcePub, myPrivate)
        if (decryptedMessage == null) {
            Logging.e(TAG, "run [-] unable to decrypt message <$decryptedMessage>")
            return ReceiveMessageResult(Status.FAILURE)
        }

        if (decryptedMessage is NegotiateSymKeyMessage) {
            enqueueDependencyTask(HandleNegotiateSymKeyTask(decryptedMessage))
        }

        if (decryptedMessage is MessageReadMessage) {
            Logging.d(TAG, "run [+] update message status <${decryptedMessage.messageSignature}>")
            MessageManager.setMessageRead(decryptedMessage.messageSignature)
        }

        forwardTask?.get().let { // just for information... we may don't need the result here
            if (it?.status == Status.SUCCESS) {
                Logging.d(TAG, "run [+] successfully forwarded message ${it?.status}")
            }
        }


        // 6. notify listeners or return result
        return ReceiveMessageResult(Status.SUCCESS, message = decryptedMessage)
    }

    override fun onUnhandledException(exception: Exception): ReceiveMessageResult {
        return ReceiveMessageResult(Status.FAILURE, exception = exception)
    }
}