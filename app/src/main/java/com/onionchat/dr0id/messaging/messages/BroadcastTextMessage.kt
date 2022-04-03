package com.onionchat.dr0id.messaging.messages

import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.MessageParseException
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.localstorage.userstore.Broadcast
import org.json.JSONObject


 // TODO check if from + label matches the broadcast id !!
@Deprecated("All messages can be broadcast messages")
class BroadcastTextMessage(
    private val broadcast: Broadcast,
    messageId: String = java.util.UUID.randomUUID().toString(),
    private val textData: TextMessageData,
    hashedFrom: String,
    hashedTo: String,
    signature: String = "",
    messageStatus: Int = 0,
    created: Long = System.currentTimeMillis(),
    read: Int = 0
) :
    SymmetricMessage(
        messageId,
        createPayload(textData, broadcast),
        hashedFrom,
        hashedTo,
        signature,
        messageStatus,
        created,
        read,
        MessageTypes.BROADCAST_TEXT_MESSAGE.ordinal
    ), IBroadcastMessage, ITextMessage {


    constructor(symmetricMessage: SymmetricMessage) : this(
        extractPayload(symmetricMessage.data),
        symmetricMessage.messageId,
        TextMessage.extractPayload(symmetricMessage.data),
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
        val BROADCAST_ID = "broadcast_id"

        @JvmStatic
        val BROADCAST_LABEL = "broadcast_label"

        fun createPayload(text: TextMessageData, broadcast: Broadcast): ByteArray {
            val content = JSONObject()
            content.put(TextMessage.PAYLOAD_TEXT, text.text)
            content.put(TextMessage.PAYLOAD_TEXT_FORMAT, text.formatInfo)
            content.put(BROADCAST_ID, broadcast.id)
            content.put(BROADCAST_LABEL, broadcast.label)
            return content.toString().toByteArray()
        }

        fun extractPayload(data: ByteArray): Broadcast {
            val content = JSONObject(String(data))
            if (!content.has(BROADCAST_ID) || !content.has(BROADCAST_LABEL)) {
                Logging.e(TAG, "extractPayload [-] error while extract payload")
                throw MessageParseException("Invalid payload", content.toString());
            }
            return Broadcast(content.getString(BROADCAST_ID), content.getString(BROADCAST_LABEL))
        }
    }

    override fun getBroadcast(): Broadcast {
        return broadcast
    }

}