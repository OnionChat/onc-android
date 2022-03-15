package com.onionchat.dr0id.messaging.messages

import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.MessageParseException
import com.onionchat.dr0id.messaging.SymmetricMessage
import org.json.JSONObject

class TextMessageData(val text: String, val formatInfo: String)

open class TextMessage(
    messageId: String = java.util.UUID.randomUUID().toString(),
    private val textData: TextMessageData,
    hashedFrom: String,
    hashedTo: String,
    signature: String = "",
    messageStatus: Int = 0,
    created: Long = System.currentTimeMillis(),
    read: Int = 0,
    type: Int = MessageTypes.TEXT_MESSAGE.ordinal
) : SymmetricMessage(
    messageId,
    createPayload(textData),
    hashedFrom,
    hashedTo,
    signature,
    messageStatus,
    created,
    read,
    type
), ITextMessage { // todo user base64


    constructor(symmetricMessage: SymmetricMessage) : this(
        symmetricMessage.messageId,
        extractPayload(symmetricMessage.data),
        symmetricMessage.hashedFrom,
        symmetricMessage.hashedTo,
        symmetricMessage.signature,
        symmetricMessage.messageStatus,
        symmetricMessage.created,
        symmetricMessage.read
    )

    override fun getText(): TextMessageData {
        return textData
    }

    companion object {

        @JvmStatic
        val PAYLOAD_TEXT = "text"

        @JvmStatic
        val PAYLOAD_TEXT_FORMAT = "format"

        fun createPayload(text: TextMessageData): ByteArray {
            val content = JSONObject()
            content.put(PAYLOAD_TEXT, text.text)
            content.put(PAYLOAD_TEXT_FORMAT, text.formatInfo)
            return content.toString().toByteArray()
        }

        fun extractPayload(data: ByteArray): TextMessageData {
            val content = JSONObject(String(data))
            if (!content.has(PAYLOAD_TEXT) || !content.has(PAYLOAD_TEXT_FORMAT)) {
                Logging.e(TAG, "extractPayload [-] error while extract payload")
                throw MessageParseException("Invalid payload", content.toString());
            }
            return TextMessageData(content.getString(PAYLOAD_TEXT), content.getString(PAYLOAD_TEXT_FORMAT))
        }
    }
}