package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.MessageProcessor
import com.onionchat.dr0id.queue.OnionFuture
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.ConversationType
import java.security.cert.Certificate

class SendMessageTask(val message: IMessage, val to: Conversation) : OnionTask<SendMessageTask.SendMessageResult>() {


    class SendMessageResult(success: Status, val sendingFuture: OnionFuture<ForwardMessageTask.ForwardMessageResult>? = null, val encryptedMessage: EncryptedMessage? = null, exception: java.lang.Exception? = null) :
        OnionTask.Result(success, exception) {

    }

    companion object {
        val TAG  = "SendMessageTask"
    }

    override fun run(): SendMessageResult {

        // 1. get crypto information

        //// my crypto
        val myPrivate = Crypto.getMyKey()

        //// target user crypto
        val certId = to.getCertId()

        var pub : Certificate? = null
        if(certId != null) {
            pub = Crypto.getPublicKey(certId)
        }

        var symKeyAlias: String? = to.getLastSymAlias()

        // 2. pack the message
        val encryptedMessage = MessageProcessor.pack(message, pub, myPrivate, symKeyAlias)
        if (encryptedMessage == null) {
            Logging.e(TAG, "run [-] unable to encrypt message $message")
            // todo throw?
            throw java.lang.Exception("Unable to pack message $message")
        }

        // 3. add message to message storage
        if (!MessageManager.storeEncryptedMessage(encryptedMessage).get()) {
            Logging.e(TAG, "run [-] unable to store message $encryptedMessage")
            // todo throw?
            throw java.lang.Exception("Unable to store message $encryptedMessage")
        }

        // 4. send message

//        val result = OnionClient.postmessage(EncryptedMessage.toJson(encryptedMessage), to.id).get() // todo forward ?
//        if(result == OnionClient.MessageSentResult.SENT) {
//            encryptedMessage.messageStatus = MessageStatus.addFlag(encryptedMessage.messageStatus, MessageStatus.SENT)
//            MessageManager.updateEncryptedMessage(encryptedMessage)
//            Logging.d(TAG, "run [+] successfully sent message $encryptedMessage")
//            return SendMessageResult(Status.SUCCESS, result, encryptedMessage)
//        }
        return SendMessageResult(Status.PENDING, enqueueFollowUpTaskPriority(ForwardMessageTask(encryptedMessage)), encryptedMessage = encryptedMessage)

    }

    override fun onUnhandledException(exception: Exception): SendMessageResult {
        return SendMessageResult(Status.FAILURE, exception = exception)
    }
}