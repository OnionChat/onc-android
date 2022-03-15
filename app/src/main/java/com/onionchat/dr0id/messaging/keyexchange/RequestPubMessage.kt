package com.onionchat.dr0id.keyexchange

import com.onionchat.common.MessageTypes
import com.onionchat.localstorage.messagestore.EncryptedMessage
import java.util.*

class RequestPubMessage(
    val hashedFrom: String,
    val hashedTo: String,
    val messageId: String = UUID.randomUUID().toString(),
    val signature: String,
    val created: Long = System.currentTimeMillis(),
    val status: Int,
    val pub: ByteArray
) {


    fun toEncryptedMessage(): EncryptedMessage {
        return EncryptedMessage(messageId, pub, hashedFrom, hashedTo, signature, 0, created, 0, MessageTypes.REQUEST_PUB_MESSAGE.ordinal)
    }

    companion object {
        fun fromEncryptedMessage(encryptedMessage: EncryptedMessage): RequestPubMessage {
            return RequestPubMessage(
                encryptedMessage.hashedFrom,
                encryptedMessage.hashedTo,
                encryptedMessage.messageId,
                encryptedMessage.signature,
                encryptedMessage.created,
                encryptedMessage.messageStatus,
                encryptedMessage.encryptedMessageBytes
            )
        }
    }
}