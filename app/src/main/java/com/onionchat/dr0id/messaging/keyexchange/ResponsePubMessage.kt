package com.onionchat.dr0id.messaging.keyexchange

import android.util.Base64
import com.onionchat.common.Crypto.byteToPub
import com.onionchat.common.Crypto.decryptAsym
import com.onionchat.common.Crypto.encryptAsym
import com.onionchat.common.Crypto.sign
import com.onionchat.common.Crypto.verify
import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.localstorage.messagestore.EncryptedMessage
import java.security.Key
import java.security.cert.Certificate

class ResponsePubMessage(
    hashedFrom: String,
    hashedTo: String,
    messageId: String = java.util.UUID.randomUUID().toString(),
    signature: String = "",
    created: Long = System.currentTimeMillis(),
    messageStatus: Int,
    val pub: Certificate,
    type: Int = MessageTypes.RESPONSE_PUB_MESSAGE.ordinal
) : IMessage(messageId, hashedFrom, hashedTo, signature, messageStatus, created, 0, type, "") {


    fun toEncryptedMessage(targetPub: Certificate, myPrivate: Key): EncryptedMessage? {
        val encryptedData = encryptAsym(targetPub, pub.encoded)  // todo add my real uid
        sign(myPrivate, encryptedData)?.let {
            signature = Base64.encodeToString(it, Base64.DEFAULT)
            return EncryptedMessage(
                messageId,
                encryptedData,
                hashedFrom,
                hashedTo,
                signature,
                0,
                created,
                0,
                type
            )
        } ?: run {
            return null
        }
    }

    companion object {

        val TAG = "ResponsePubMessage"

        fun fromEncryptedMessage(encryptedMessage: EncryptedMessage, myPrivate: Key): ResponsePubMessage? {
            decryptAsym(myPrivate, encryptedMessage.encryptedMessageBytes)?.let { pub ->
                // verify even the pub was shipped within the message
                byteToPub(pub)?.let { cert ->
                    return if (verify(cert, encryptedMessage.encryptedMessageBytes, Base64.decode(encryptedMessage.signature, Base64.DEFAULT))) {
                        ResponsePubMessage(
                            encryptedMessage.hashedFrom,
                            encryptedMessage.hashedTo,
                            encryptedMessage.messageId,
                            encryptedMessage.signature,
                            encryptedMessage.created,
                            encryptedMessage.messageStatus,
                            cert
                        )
                    } else {
                        Logging.e(TAG, "Error while verify message")
                        null
                    }

                } ?: run {
                    Logging.e(TAG, "Error while convert certificate")
                    return null
                }

            } ?: run {
                Logging.e(TAG, "Decryption error")
                return null
            }
        }
    }

    override fun getMessageType(): Int {
        TODO("Not yet implemented")
    }
}