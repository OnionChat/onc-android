package com.onionchat.dr0id.messaging.messages

import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.IMessage

class InvalidMessage(private val text: String = "") : ITextMessage, IMessage(hashedFrom = "", hashedTo = "", type = 0) { // get from real message
    override fun getText(): TextMessageData {
        return TextMessageData("Error <$text>", "")
    }

    override fun getMessageType(): Int {
        return MessageTypes.INVALID_MESSAGE.ordinal
    }
}