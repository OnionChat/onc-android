package com.onionchat.localstorage.userstore

import android.util.Base64
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import org.json.JSONObject

@Entity
data class Broadcast(@PrimaryKey val id: String, @ColumnInfo(name = "label") val label: String) {


    fun getHashedId(): String {
        return id
    }


    /*
    !! TODO TEMPORARY HACK !!
     */
    @Ignore
    var real_label: String? = null

    @Ignore
    var sym_key_alias: String? = null

    @Ignore
    var sym_key: ByteArray? = null

    @Ignore
    var pub: ByteArray? = null

    @Ignore
    var pub_alias: String? = null

    @Ignore
    var signature: ByteArray? = null

    init {
        try {
            val json = JSONObject(label)
            real_label = json.getString(PAYLOAD_LABEL)
            sym_key_alias = json.getString(PAYLOAD_SYM_ALIAS)
            sym_key = Base64.decode(json.getString(PAYLOAD_SYM_KEY), Base64.DEFAULT)
            pub = Base64.decode(json.getString(PAYLOAD_PUB), Base64.DEFAULT)
            pub_alias = json.getString(PAYLOAD_PUB_ALIAS)
            signature = Base64.decode(json.getString(PAYLOAD_SIGNATURE), Base64.DEFAULT)
            Logging.e(TAG, "init [+] decoded broadcast <$label, $sym_key_alias> ")

        } catch (e: java.lang.Exception) {
            Logging.e(TAG, "init [-] error while initialize Broadcast <$label>")
            real_label = "ERROR"
        }
    }


    companion object {

        fun createPayload(broadcast: Broadcast): String {
            val json = JSONObject()
            json.put(PAYLOAD_LABEL, broadcast.real_label)
            json.put(PAYLOAD_ID, broadcast.id)
            json.put(PAYLOAD_SYM_KEY, Base64.encodeToString(broadcast.sym_key, Base64.DEFAULT))
            json.put(PAYLOAD_SYM_ALIAS, broadcast.sym_key_alias)
            json.put(PAYLOAD_PUB, Base64.encodeToString(broadcast.pub, Base64.DEFAULT))
            json.put(PAYLOAD_PUB_ALIAS, broadcast.pub_alias)
            json.put(PAYLOAD_SIGNATURE, Base64.encodeToString(broadcast.signature, Base64.DEFAULT))
            return json.toString()
        }

        fun createFromPayload(content: String): Broadcast? {
            try {
                val json = JSONObject(content)
                if (
                    !json.has(PAYLOAD_LABEL) ||
                    !json.has(PAYLOAD_ID) ||
                    !json.has(PAYLOAD_SYM_KEY) ||
                    !json.has(PAYLOAD_SYM_ALIAS) ||
                    !json.has(PAYLOAD_PUB) ||
                    !json.has(PAYLOAD_PUB_ALIAS) ||
                    !json.has(PAYLOAD_SIGNATURE)
                ) {
                    Logging.e(TAG, "createFromPayload [-] invalid json ${content}")
                    return null
                }

                return Broadcast(json.getString(PAYLOAD_ID), content)
            } catch (e: Exception) {
                Logging.e(TAG, "createFromPayload [-] invalid json ${content}", e)
                return null
            }
        }

        fun generateId(label: String, uid: String): String {
            return IDGenerator.toHashedId(label + uid)
        }

        const val PAYLOAD_LABEL = "broadcast_label"
        const val PAYLOAD_ID = "broadcast_id"
        const val PAYLOAD_SYM_KEY = "broadcast_sym_key"
        const val PAYLOAD_SYM_ALIAS = "broadcast_sym_alias"
        const val PAYLOAD_PUB = "broadcast_pub"
        const val PAYLOAD_PUB_ALIAS = "broadcast_pub_alias"
        const val PAYLOAD_SIGNATURE = "broadcast_signature"
        const val TAG = "Broadcast"
    }
}