package com.onionchat.common

import android.util.Base64
import org.json.JSONObject

data class QrPayload(val uid: String, val pubKey: ByteArray) {


    companion object {
        fun encode(payload : QrPayload) : String {
            val pubKeyBase64 = Base64.encodeToString(payload.pubKey, Base64.DEFAULT)
            val root = JSONObject()
            root.put("uid", payload.uid)
            root.put("pubKey", pubKeyBase64)
            val message = root.toString()
            return message
        }

        fun decode(qrString : String) : QrPayload {
            val root = JSONObject(qrString)
            val uid = root.getString("uid")
            val pubKeyBase64 = root.getString("pubKey")
            val pubKey = Base64.decode(pubKeyBase64, Base64.DEFAULT)
            val payload = QrPayload(uid, pubKey)
            return payload
        }
    }

}