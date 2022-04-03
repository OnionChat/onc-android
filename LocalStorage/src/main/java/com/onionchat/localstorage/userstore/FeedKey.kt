package com.onionchat.localstorage.userstore

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onionchat.common.Crypto

@Entity
data class FeedKey(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "encrypted_key") val encryptedKey: ByteArray, // encrypted with asym key material
    @ColumnInfo(name = "alias") val alias: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long
) {


    fun getDecryptedKey(): ByteArray? {
        val myPrivate = Crypto.getMyKey()
        return Crypto.decryptAsym(myPrivate, encryptedKey)
    }
}