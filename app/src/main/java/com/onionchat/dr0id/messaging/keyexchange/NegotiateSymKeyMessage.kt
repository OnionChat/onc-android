package com.onionchat.dr0id.messaging.keyexchange


import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.AsymmetricMessage
import com.onionchat.localstorage.userstore.SymAlias
import org.json.JSONObject
import java.util.*
import javax.crypto.SecretKey

class ResponseSymMessageData(
    val alias: SymAlias,
    val key: ByteArray
)

class ResponseSymMessage(
    hashedFrom: String,
    hashedTo: String,
    messageId: String = UUID.randomUUID().toString(),
    signature: String = "",
    created: Long = System.currentTimeMillis(),
    status: Int = 0,
    data: ResponseSymMessageData,
    read: Int = 0,
    type: Int = MessageTypes.SYM_KEY_MESSAGE.ordinal,
    extra: String = ""
) : AsymmetricMessage(
    messageId,
    createPayload(data)!! /* Let's throw an exception!*/,
    hashedFrom,
    hashedTo,
    signature,
    status,
    created,
    read,
    type,
    extra
) {

    constructor(asymmetricMessage: AsymmetricMessage) : this(
        asymmetricMessage.hashedFrom,
        asymmetricMessage.hashedTo,
        asymmetricMessage.messageId,
        asymmetricMessage.signature,
        asymmetricMessage.created,
        asymmetricMessage.messageStatus,
        extractPayload(String(asymmetricMessage.data))!! /*Let's throw an exception!*/,
        asymmetricMessage.read,
        asymmetricMessage.type,
        asymmetricMessage.extra
    )

    companion object {

        val PAYLOAD_KEY = "key"
        val PAYLOAD_ALIAS = "alias"
        val PAYLOAD_TIMESTAMP = "timestamp"
        val PAYLOAD_ID = "id"
        val PAYLOAD_UID = "uid"

        fun createPayload(data: ResponseSymMessageData): ByteArray? {
            val content = JSONObject()
            content.put(PAYLOAD_KEY, android.util.Base64.encodeToString(data.key, android.util.Base64.DEFAULT))
            content.put(PAYLOAD_ALIAS, data.alias.alias)
            content.put(PAYLOAD_TIMESTAMP, data.alias.timestamp)
            content.put(PAYLOAD_ID, data.alias.id)
            content.put(PAYLOAD_UID, data.alias.userid)
            return content.toString().toByteArray()
        }

        fun extractPayload(json: String): ResponseSymMessageData? {
            val content = JSONObject(json)
            if (!content.has(PAYLOAD_ALIAS)
                || !content.has(PAYLOAD_KEY)
                || !content.has(PAYLOAD_TIMESTAMP)
                || !content.has(PAYLOAD_ID)
            ) {
                Logging.e(TAG, "fromAsymmetricMessage [-] invalid payload <$json>")
                return null
            }
            val alias = content.getString(PAYLOAD_ALIAS)
            if (alias.isEmpty()) {
                Logging.e(TAG, "fromAsymmetricMessage [-] invalid alias <$json>")
                return null
            }
            val encodedKey = android.util.Base64.decode(content.getString(PAYLOAD_KEY), android.util.Base64.DEFAULT)
            if (encodedKey.isEmpty()) {
                Logging.e(TAG, "fromAsymmetricMessage [-] invalid encodedKey <$json>")
                return null
            }
            val timestamp = content.getLong(PAYLOAD_TIMESTAMP)
            if(timestamp <= 0L) {
                Logging.e(TAG, "fromAsymmetricMessage [-] invalid timestamp <$json>")
                return null
            }
            val id = content.getString(PAYLOAD_ID)
            if(id.isEmpty()) {
                Logging.e(TAG, "fromAsymmetricMessage [-] invalid id <$json>")
                return null
            }
            val uid = content.getString(PAYLOAD_UID)
            if(uid.isEmpty()) {
                Logging.e(TAG, "fromAsymmetricMessage [-] invalid uid <$json>")
                return null
            }

            return ResponseSymMessageData(SymAlias(id, uid, alias, timestamp), encodedKey)
        }

//        fun fromAsymmetricMessage(asymmetricMessage: AsymmetricMessage): SymKeyMessage? {
//            String(asymmetricMessage.data).let { decoded ->
//                val alias = extractPayload(decoded) ?: return null
//
//                return SymKeyMessage(
//                    asymmetricMessage.hashedFrom,
//                    asymmetricMessage.hashedTo,
//                    asymmetricMessage.messageId,
//                    asymmetricMessage.signature,
//                    asymmetricMessage.created,
//                    asymmetricMessage.messageStatus,
//                    alias,
//                    asymmetricMessage.read,
//                    asymmetricMessage.type,
//                    asymmetricMessage.extra
//                )
//            }
//        }
    }
}