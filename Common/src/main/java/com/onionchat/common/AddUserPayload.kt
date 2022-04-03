package com.onionchat.common

import android.util.Base64
import org.json.JSONObject
import java.lang.Exception

data class AddUserPayload(val uid: String, val pubKey: ByteArray, val label: String) {


    companion object {

        val PAYLOAD_UID = "uid"
        val PAYLOAD_PUB = "pub"
        val PAYLOAD_LABEL = "label"

        const val TAG = "AddUserPayload"

        fun encode(payload: AddUserPayload): String {
            val pubKeyBase64 = Base64.encodeToString(payload.pubKey, Base64.DEFAULT)
            val root = JSONObject()
            root.put(PAYLOAD_UID, payload.uid)
            root.put(PAYLOAD_PUB, pubKeyBase64)
            root.put(PAYLOAD_LABEL, payload.label)
            val message = root.toString()
            Logging.v(TAG, "encode message <${message}>")
            return message
        }

        fun decode(qrString: String): AddUserPayload? {
            try {
                val root = JSONObject(qrString)
                if (!root.has(PAYLOAD_UID) ||
                    !root.has(PAYLOAD_PUB) ||
                    !root.has(PAYLOAD_LABEL)
                ) {
                    Logging.e(TAG, "Unable to decode payload $qrString")
                    return null
                }
                val uid = root.getString(PAYLOAD_UID)
                val pubKeyBase64 = root.getString(PAYLOAD_PUB)
                val pubKey = Base64.decode(pubKeyBase64, Base64.DEFAULT)
                val label = root.getString(PAYLOAD_LABEL)
                val payload = AddUserPayload(uid, pubKey, label)
                return payload
            }catch (exception:Exception) {
                Logging.e(TAG, "Unable to decode payload $qrString", exception)
                return null
            }
        }
    }

}