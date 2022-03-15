package com.onionchat.dr0id.messaging

import android.util.Base64
import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.dr0id.messaging.keyexchange.ResponsePubMessage
import com.onionchat.dr0id.messaging.messages.ITextMessage
import com.onionchat.dr0id.messaging.messages.TextMessageData
import com.onionchat.localstorage.messagestore.EncryptedMessage
import java.security.Key
import java.security.cert.Certificate

open class AsymmetricMessage(
    messageId: String = java.util.UUID.randomUUID().toString(),
    var cipher: ByteArray,
    hashedFrom: String,
    hashedTo: String,
    signature: String = "",
    messageStatus: Int = 0,
    created: Long = System.currentTimeMillis(),
    read: Int = 0,
    type: Int,
    extra: String = ""
) : IMessage(messageId, hashedFrom, hashedTo, signature, messageStatus, created, read, type, extra) {


    companion object {

        val TAG = "AsymmetricMessage"

        fun encrypt(asymmetricMessage: AsymmetricMessage, targetPub: Certificate, myPrivate: Key): EncryptedMessage? {
            val encryptedData = Crypto.encryptAsym(targetPub, asymmetricMessage.cipher)
            Crypto.sign(myPrivate, encryptedData)?.let {
                asymmetricMessage.signature = Base64.encodeToString(it, Base64.DEFAULT)
                return EncryptedMessage(
                    asymmetricMessage.messageId,
                    encryptedData,
                    asymmetricMessage.hashedFrom,
                    asymmetricMessage.hashedTo,
                    asymmetricMessage.signature,
                    asymmetricMessage.messageStatus,
                    asymmetricMessage.created,
                    asymmetricMessage.read,
                    asymmetricMessage.type,
                    asymmetricMessage.extra
                )
            } ?: run {
                Logging.e(TAG, "Unable to sign message")
                return null
            }
        }

        fun decrypt(encryptedMessage: EncryptedMessage, myPrivate: Key, sourcePub: Certificate): AsymmetricMessage? {
            Crypto.decryptAsym(myPrivate, encryptedMessage.encryptedMessageBytes)?.let { decryptedData ->
                if (Crypto.verify(sourcePub, encryptedMessage.encryptedMessageBytes, Base64.decode(encryptedMessage.signature, Base64.DEFAULT))) {
                    return AsymmetricMessage(
                        encryptedMessage.messageId,
                        decryptedData,
                        encryptedMessage.hashedFrom,
                        encryptedMessage.hashedTo,
                        encryptedMessage.signature,
                        encryptedMessage.messageStatus,
                        encryptedMessage.created,
                        encryptedMessage.read,
                        encryptedMessage.type,
                        encryptedMessage.extra
                    )
                } else {
                    Logging.e(ResponsePubMessage.TAG, "Error while verify message")
                }
            } ?: run {
                Logging.e(ResponsePubMessage.TAG, "Error while decrypt message")
            }
            return null
        }
    }

    override fun getMessageType(): Int {
        return type
    }
}