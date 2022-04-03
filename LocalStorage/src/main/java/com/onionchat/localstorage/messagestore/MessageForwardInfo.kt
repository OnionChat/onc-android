package com.onionchat.localstorage.messagestore

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.onionchat.localstorage.userstore.User

@Entity(foreignKeys = arrayOf(
    ForeignKey(entity = EncryptedMessage::class,
        parentColumns = arrayOf("message_id"),
        childColumns = arrayOf("message_id"),
        onDelete = ForeignKey.CASCADE)
))
data class MessageForwardInfo(@PrimaryKey val id: String, @ColumnInfo( index = true) val message_id: String, @ColumnInfo(name = "user_id") /* !! THE HASHED USERID !!*/ val userId: String, @ColumnInfo(name = "timestamp") val timestamp: Long)