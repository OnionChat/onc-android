package com.onionchat.dr0id.messaging.messages

import android.util.Base64
import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.MessageParseException
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.dr0id.messaging.data.Attachment
import org.json.JSONObject


open class AttachmentMessage(
    messageId: String = java.util.UUID.randomUUID().toString(),
    private val attachment: Attachment,
    hashedFrom: String,
    hashedTo: String,
    signature: String = "",
    messageStatus: Int = 0,
    created: Long = System.currentTimeMillis(),
    read: Int = 0,
    type: Int = MessageTypes.ATTACHMENT_MESSAGE.ordinal,
    extras :String = ""
) : SymmetricMessage(
    messageId,
    createPayload(attachment),
    hashedFrom,
    hashedTo,
    signature,
    messageStatus,
    created,
    read,
    type,
    extras
), IAttachmentMessage, IQuotableMessage { // todo user base64


    constructor(symmetricMessage: SymmetricMessage) : this(
        symmetricMessage.messageId,
        extractPayload(symmetricMessage.data),
        symmetricMessage.hashedFrom,
        symmetricMessage.hashedTo,
        symmetricMessage.signature,
        symmetricMessage.messageStatus,
        symmetricMessage.created,
        symmetricMessage.read,
        extras = symmetricMessage.extra
    )


    companion object {

        @JvmStatic
        val PAYLOAD_ATTACHMENT_ID = "attachment_id"

        @JvmStatic
        val PAYLOAD_ATTACHMENT_MIMETYPE = "attachment_mimetype"

        @JvmStatic
        val PAYLOAD_ATTACHMENT_SIGNATURE = "attachment_signature"

        @JvmStatic
        val PAYLOAD_ATTACHMENT_SIZE = "attachment_size"

        @JvmStatic
        val PAYLOAD_ATTACHMENT_THUMBNAIL = "attachment_thumbnail"

        @JvmStatic
        val PAYLOAD_KEY = "key"

        @JvmStatic
        val PAYLOAD_IV = "iv"

        @JvmStatic
        val PAYLOAD_ALIAS = "alias"

        @JvmStatic
        val PAYLOAD_QUOTED_MESSAGE_ID = "quoted_message_id"

        fun createPayload(metaData: Attachment): ByteArray {
            val content = JSONObject()
            content.put(PAYLOAD_ATTACHMENT_ID, metaData.attachmentId)
            content.put(PAYLOAD_ATTACHMENT_MIMETYPE, metaData.mimetype)
            content.put(PAYLOAD_ATTACHMENT_SIGNATURE, Base64.encodeToString(metaData.signature, Base64.DEFAULT))
            metaData.thumbnail?.let {
                content.put(PAYLOAD_ATTACHMENT_THUMBNAIL, Base64.encodeToString(it, Base64.DEFAULT))
            }
            content.put(PAYLOAD_ATTACHMENT_SIZE, metaData.size)
            content.put(PAYLOAD_KEY, Base64.encodeToString(metaData.key, Base64.DEFAULT))
            content.put(PAYLOAD_IV, Base64.encodeToString(metaData.iv, Base64.DEFAULT))
            content.put(PAYLOAD_ALIAS, metaData.alias)
            content.put(PAYLOAD_QUOTED_MESSAGE_ID, metaData.quotedMessageId)
            return content.toString().toByteArray()
        }

        fun extractPayload(data: ByteArray): Attachment {
            val content = JSONObject(String(data))
            if (!content.has(PAYLOAD_ATTACHMENT_ID) ||
                !content.has(PAYLOAD_ATTACHMENT_MIMETYPE) ||
                !content.has(PAYLOAD_ATTACHMENT_SIGNATURE) ||
                !content.has(PAYLOAD_ATTACHMENT_SIZE) ||
                !content.has(PAYLOAD_KEY) ||
                !content.has(PAYLOAD_IV) ||
                !content.has(PAYLOAD_ALIAS) ||
                !content.has(PAYLOAD_QUOTED_MESSAGE_ID)
            ) {
                Logging.e(TAG, "extractPayload [-] error while extract payload")
                throw MessageParseException("Invalid payload", content.toString());
            }
            var quotedMessageId = ""
            if (content.has(PAYLOAD_QUOTED_MESSAGE_ID)) {
                quotedMessageId = content.getString(PAYLOAD_QUOTED_MESSAGE_ID)
            }
            var thumbnail: ByteArray? = null
            if (content.has(PAYLOAD_ATTACHMENT_THUMBNAIL)) {
                thumbnail = Base64.decode(content.getString(PAYLOAD_ATTACHMENT_THUMBNAIL), Base64.DEFAULT)
            }
            return Attachment(
                content.getString(PAYLOAD_ATTACHMENT_ID),
                content.getString(PAYLOAD_ATTACHMENT_MIMETYPE),
                Base64.decode(content.getString(PAYLOAD_ATTACHMENT_SIGNATURE), Base64.DEFAULT),
                content.getInt(PAYLOAD_ATTACHMENT_SIZE),
                Base64.decode(content.getString(PAYLOAD_KEY), Base64.DEFAULT),
                Base64.decode(content.getString(PAYLOAD_IV), Base64.DEFAULT),
                content.getString(PAYLOAD_ALIAS),
                thumbnail,
                quotedMessageId
            )
        }
    }

    override fun getQuotedMessageId(): String {
        return attachment.quotedMessageId
    }

    override fun getAttachment(): Attachment {
        return attachment
    }
}