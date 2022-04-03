package com.onionchat.dr0id.messaging.messages

import android.util.Base64
import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.localstorage.userstore.ContactDetails
import org.json.JSONObject

class ProvideContactDetailsMessage(
    val contactDetails: ContactDetails,
    messageId: String = java.util.UUID.randomUUID().toString(),
    hashedFrom: String,
    hashedTo: String,
    signature: String = "",
    messageStatus: Int = 0,
    created: Long = System.currentTimeMillis(),
    read: Int = 0,
    type: Int = MessageTypes.PROVIDE_CONTACT_DETAILS_MESSAGE.ordinal
) :
    SymmetricMessage(
        messageId,
        createPayload(contactDetails),
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
        const val PAYLOAD_USER_ID = "user_id"
        const val PAYLOAD_ALIAS = "alias"
        const val PAYLOAD_AVATAR = "avatar"
        const val PAYLOAD_EXTRA = "extra"
        const val PAYLOAD_TIMESTAMP = "timestamp"

        fun createPayload(contactDetails: ContactDetails): ByteArray {
            val json = JSONObject()
            json.put(PAYLOAD_ID, contactDetails.id)
            json.put(PAYLOAD_USER_ID, contactDetails.userid)
            json.put(PAYLOAD_ALIAS, contactDetails.alias)
            json.put(PAYLOAD_AVATAR, Base64.encodeToString(contactDetails.avatar, Base64.DEFAULT))
            json.put(PAYLOAD_EXTRA, contactDetails.extra)
            json.put(PAYLOAD_TIMESTAMP, contactDetails.timestamp)
            return json.toString().toByteArray()
        }

        fun extractPayload(data: ByteArray): ContactDetails? {
            val json = JSONObject(String(data))
            if (!json.has(PAYLOAD_ID) ||
                !json.has(PAYLOAD_USER_ID) || // todo validate if it's correct user id !!!
                !json.has(PAYLOAD_ALIAS) ||
                !json.has(PAYLOAD_AVATAR) ||
                !json.has(PAYLOAD_EXTRA) ||
                !json.has(PAYLOAD_TIMESTAMP)
            ) {
                Logging.e(TAG, "extractPayload [-] invalid json data $json")
                return null
            }

            return ContactDetails(json.getString(PAYLOAD_ID),
            json.getString(PAYLOAD_USER_ID),
            json.getString(PAYLOAD_ALIAS),
            Base64.decode(json.getString(PAYLOAD_AVATAR), Base64.DEFAULT),
            json.getString(PAYLOAD_EXTRA),
            json.getLong(PAYLOAD_TIMESTAMP))
        }
    }
}