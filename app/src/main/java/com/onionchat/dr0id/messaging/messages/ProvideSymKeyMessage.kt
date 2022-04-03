package com.onionchat.dr0id.messaging.messages

import android.util.Base64
import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.SymmetricMessage
import org.json.JSONObject

data class SymKeyPayload(val id: String, val alias: String, val encryptedKey: ByteArray, val timestamp: Long)

class ProvideSymKeyMessage(
    val symKey: SymKeyPayload,
    messageId: String = java.util.UUID.randomUUID().toString(),
    hashedFrom: String,
    hashedTo: String,
    signature: String = "",
    messageStatus: Int = 0,
    created: Long = System.currentTimeMillis(),
    read: Int = 0,
    type: Int = MessageTypes.PROVIDE_SYM_KEY_MESSAGE.ordinal
) :
    SymmetricMessage(
        messageId,
        createPayload(symKey),
        hashedFrom,
        hashedTo,
        signature,
        messageStatus,
        created,
        read,
        type
    ) {

    constructor(symmetricMessage: SymmetricMessage) : this(
        extractPayload(symmetricMessage.data)!!, // we'll crash... you should catch this exception dude
        symmetricMessage.messageId,
        symmetricMessage.hashedFrom,
        symmetricMessage.hashedTo,
        symmetricMessage.signature,
        symmetricMessage.messageStatus,
        symmetricMessage.created,
        symmetricMessage.read
    )

    companion object {
        const val TAG = "ProvideContactDetailsMessage"


        const val PAYLOAD_ID = "id"
        const val PAYLOAD_ALIAS = "alias"
        const val PAYLOAD_ENCRYPTED_KEY = "avatar"
        const val PAYLOAD_TIMESTAMP = "timestamp"

        fun createPayload(symKey: SymKeyPayload): ByteArray {
            val json = JSONObject()
            json.put(PAYLOAD_ID, symKey.id)
            json.put(PAYLOAD_ALIAS, symKey.alias)
            json.put(PAYLOAD_ENCRYPTED_KEY, Base64.encodeToString(symKey.encryptedKey, Base64.DEFAULT))
            json.put(PAYLOAD_TIMESTAMP, symKey.timestamp)
            return json.toString().toByteArray()
        }

        fun extractPayload(data: ByteArray): SymKeyPayload? {
            val json = JSONObject(String(data))
            if (!json.has(PAYLOAD_ID) ||// todo validate if it's correct user id !!!
                !json.has(PAYLOAD_ALIAS) ||
                !json.has(PAYLOAD_ENCRYPTED_KEY) ||
                !json.has(PAYLOAD_TIMESTAMP)
            ) {
                Logging.e(TAG, "extractPayload [-] invalid json data $json")
                return null
            }

            return SymKeyPayload(
                json.getString(PAYLOAD_ID),
                json.getString(PAYLOAD_ALIAS),
                Base64.decode(json.getString(PAYLOAD_ENCRYPTED_KEY), Base64.DEFAULT),
                json.getLong(PAYLOAD_TIMESTAMP)
            )
        }
    }
}