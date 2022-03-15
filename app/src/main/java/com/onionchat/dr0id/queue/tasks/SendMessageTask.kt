package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.common.MessageStatus
import com.onionchat.connector.http.OnionClient
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.MessageProcessor
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.userstore.User

class SendMessageTask(val message: IMessage, fromUID: String, val to: User) : OnionTask<SendMessageTask.SendMessageResult>() {


    class SendMessageResult(success: Status, sentResult: OnionClient.MessageSentResult? = null, exception: java.lang.Exception? = null) :
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
        val pub = Crypto.getPublicKey(to.certId)
        if (pub == null) {
            Logging.d(TAG, "run [-] no asymmetric key found for user $to")
        }

        var symKeyAlias: String? = null
        if (to.symaliases == null || to.symaliases!!.isEmpty()) {
            Logging.d(TAG, "run [-] no symmetric key found for user $to")
        } else {
            symKeyAlias = to.getLastAlias()!!.alias // todo check if timestamp sort is correct
        }

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
        val result = OnionClient.postmessage(EncryptedMessage.toJson(encryptedMessage), to.id).get() // todo forward ?
        if(result == OnionClient.MessageSentResult.SENT) {
            encryptedMessage.messageStatus = MessageStatus.addFlag(encryptedMessage.messageStatus, MessageStatus.SENT)
            MessageManager.updateEncryptedMessage(encryptedMessage)
            Logging.d(TAG, "run [+] successfully sent message $encryptedMessage")
            return SendMessageResult(Status.SUCCESS, result)
        }
        return SendMessageResult(Status.FAILURE, result)

    }

    override fun onUnhandledException(exception: Exception): SendMessageResult {
        return SendMessageResult(Status.FAILURE, exception = exception)
    }
}