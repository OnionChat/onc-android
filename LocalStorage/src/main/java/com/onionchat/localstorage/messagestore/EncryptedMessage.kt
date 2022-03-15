package com.onionchat.localstorage.messagestore

import android.util.Base64
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onionchat.common.Logging
import org.json.JSONObject


@Entity
data class EncryptedMessage(
    @PrimaryKey @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "message_data") val encryptedMessageBytes: ByteArray,
    @ColumnInfo(name = "hashed_from") val hashedFrom: String,
    @ColumnInfo(name = "hashed_to") val hashedTo: String,
    @ColumnInfo(name = "signature") var signature: String,
    @ColumnInfo(name = "status") var messageStatus: Int,
    @ColumnInfo(name = "created") var created: Long,
    @ColumnInfo(name = "read") var read: Int = 0,
    @ColumnInfo(name = "type") var type: Int,
    @ColumnInfo(name = "extra") var extra: String = ""
) {


    companion object {

        const val TAG = "EncryptedMessage"
        fun toJson(encryptedMessage: EncryptedMessage): String {
            Logging.d(TAG, "toJson [+] encode $encryptedMessage")
            val content = JSONObject()
            content.put("message_id", encryptedMessage.messageId)
            content.put("message_data", Base64.encodeToString(encryptedMessage.encryptedMessageBytes, Base64.DEFAULT))
            content.put("hashed_from", encryptedMessage.hashedFrom)
            content.put("hashed_to", encryptedMessage.hashedTo)
            content.put("signature", encryptedMessage.signature)
            content.put("created", encryptedMessage.created)
            content.put("type", encryptedMessage.type)
            content.put("extra", encryptedMessage.extra)
            return content.toString()
        }

        fun fromJson(json: String): EncryptedMessage {
            Logging.d(TAG, "fromJson [+] decode <$json>")
            val content = JSONObject(json)
            return EncryptedMessage(
                content.getString("message_id"),
                Base64.decode(content.getString("message_data"), Base64.DEFAULT),
                content.getString("hashed_from"),
                content.getString("hashed_to"),
                content.getString("signature"),
                3 /*TODO create constants*/,
                content.getLong("created"),
                type = content.getInt("type"),
                extra = content.getString("extra")
            )
        }
    }
}