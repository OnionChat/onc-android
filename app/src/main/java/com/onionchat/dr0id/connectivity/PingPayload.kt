package com.onionchat.dr0id.connectivity

import com.onionchat.common.Crypto
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.dr0id.database.UserManager
import org.json.JSONObject


class PingPayload(val purpose: String = PURPOSE_DEFAULT, val uid: String = "") {


    companion object {
        const val EXTRA_HASHED_UID = "hashed_uid"
        const val EXTRA_PURPOSE = "purpose"
        const val EXTRA_VERSION = "version"

        const val PURPOSE_DEFAULT = "DEFAULT"
        const val PURPOSE_STREAM = "STREAM" // enum ?

        const val VERSION = "1"

        const val TAG = "PingPayload"

        fun encode(payload: PingPayload): String? {
            val json = JSONObject()
            UserManager.myId?.let {
                json.put(EXTRA_HASHED_UID, IDGenerator.toHashedId(it)) // todo make hashed !!
                json.put(EXTRA_PURPOSE, payload.purpose)
                json.put(EXTRA_VERSION, VERSION)
                return@encode json.toString()
            }
            return null
        }

        fun decode(jsonStr: String): PingPayload? {
            val json = JSONObject(jsonStr)
            if (!json.has(EXTRA_HASHED_UID) ||
                !json.has(EXTRA_PURPOSE) ||
                !json.has(EXTRA_VERSION)
            ) {
                Logging.e(ConnectionManager.TAG, "Error while decode json $json")
                return null
            }
            if(json.get(EXTRA_VERSION) != VERSION) {
                Logging.d(TAG, "!! WARNING !! version doesn't match")
            }
            return PingPayload(json.getString(EXTRA_PURPOSE), json.getString(EXTRA_HASHED_UID))
        }
    }
}