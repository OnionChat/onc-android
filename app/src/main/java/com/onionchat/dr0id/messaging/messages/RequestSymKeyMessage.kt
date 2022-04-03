package com.onionchat.dr0id.messaging.messages

import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.SymmetricMessage

class RequestSymKeyMessage(
    val alias: String, // currently unused
    messageId: String = java.util.UUID.randomUUID().toString(),
    hashedFrom: String,
    hashedTo: String,
    signature: String = "",
    messageStatus: Int = 0,
    created: Long = System.currentTimeMillis(),
    read: Int = 0,
    type: Int = MessageTypes.REQUEST_SYN_KEY_MESSAGE.ordinal
) :
    SymmetricMessage(
        messageId,
        alias.toByteArray(),
        hashedFrom,
        hashedTo,
        signature,
        messageStatus,
        created,
        read,
        type
    ) {

    constructor(symmetricMessage: SymmetricMessage) : this(
        String(symmetricMessage.data),
        symmetricMessage.messageId,
        symmetricMessage.hashedFrom,
        symmetricMessage.hashedTo,
        symmetricMessage.signature,
        symmetricMessage.messageStatus,
        symmetricMessage.created,
        symmetricMessage.read
    )
}