package com.onionchat.dr0id.messaging.keyexchange


import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.AsymmetricMessage
import com.onionchat.localstorage.userstore.SymAlias
import org.json.JSONObject
import java.util.*

class NegotiateSymKeyMessageData(
    val alias: SymAlias,
    val key: ByteArray
)

class NegotiateSymKeyMessage(
    hashedFrom: String,
    hashedTo: String,
    messageId: String = UUID.randomUUID().toString(),
    signature: String = "",
    created: Long = System.currentTimeMillis(),
    status: Int = 0,
    val keyData: NegotiateSymKeyMessageData,
    read: Int = 0,
    type: Int = MessageTypes.SYM_KEY_MESSAGE.ordinal,
    extra: String = ""
) : AsymmetricMessage(
    messageId,
    createPayload(keyData)!! /* Let's throw an exception!*/,
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
        extractPayload(String(asymmetricMessage.cipher))!! /*Let's throw an exception!*/,
        asymmetricMessage.read,
        asymmetricMessage.type,
        asymmetricMessage.extra
    )

    companion object {
        val TAG = "NegotiateSymKeyMessage"

        val PAYLOAD_KEY = "key"
        val PAYLOAD_ALIAS = "alias"
        val PAYLOAD_TIMESTAMP = "timestamp"
        val PAYLOAD_ID = "id"
        val PAYLOAD_UID = "uid"

        fun createPayload(data: NegotiateSymKeyMessageData): ByteArray? {
            val content = JSONObject()
            content.put(PAYLOAD_KEY, android.util.Base64.encodeToString(data.key, android.util.Base64.DEFAULT))
            content.put(PAYLOAD_ALIAS, data.alias.alias)
            content.put(PAYLOAD_TIMESTAMP, data.alias.timestamp)
            content.put(PAYLOAD_ID, data.alias.id)
            content.put(PAYLOAD_UID, UserManager.myId!!) // we have to put our uid here!
            Logging.d(TAG, "createPayload [+] created $content <${data.key.size}>")
            return content.toString().toByteArray()
        }

        fun extractPayload(json: String): NegotiateSymKeyMessageData? {
            val content = JSONObject(json)
            if (!content.has(PAYLOAD_ALIAS)
                || !content.has(PAYLOAD_KEY)
                || !content.has(PAYLOAD_TIMESTAMP)
                || !content.has(PAYLOAD_ID)
                || !content.has(PAYLOAD_UID)
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
            if (timestamp <= 0L) {
                Logging.e(TAG, "fromAsymmetricMessage [-] invalid timestamp <$json>")
                return null
            }
            val id = content.getString(PAYLOAD_ID)
            if (id.isEmpty()) {
                Logging.e(TAG, "fromAsymmetricMessage [-] invalid id <$json>")
                return null
            }
            val uid = content.getString(PAYLOAD_UID)
            if (uid.isEmpty()) {
                Logging.e(TAG, "fromAsymmetricMessage [-] invalid uid <$json>")
                return null
            }
            Logging.d(TAG, "extractPayload [+] got $content <${encodedKey.size}>")

            return NegotiateSymKeyMessageData(SymAlias(id, uid, alias, timestamp), encodedKey)
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