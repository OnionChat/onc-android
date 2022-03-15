package com.onionchat.dr0id.messaging.keyexchange

import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.localstorage.messagestore.EncryptedMessage
import java.util.*

class RequestPubMessage(
    hashedFrom: String,
    hashedTo: String,
    messageId: String = UUID.randomUUID().toString(),
    signature: String,
    created: Long = System.currentTimeMillis(),
    status: Int,
    val pub: ByteArray, // todo add my real uid
    type: Int = MessageTypes.REQUEST_PUB_MESSAGE.ordinal
) : IMessage(messageId, hashedFrom, hashedTo, signature, status, created, 0, type, "") {


    fun toEncryptedMessage(): EncryptedMessage {
        return EncryptedMessage(messageId, pub, hashedFrom, hashedTo, signature, 0, created, 0, type)
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

    override fun getMessageType(): Int {
        return type
    }
}