package com.onionchat.localstorage.userstore

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(
    foreignKeys = arrayOf(
        ForeignKey(
            entity = User::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("userid"),
            onDelete = ForeignKey.CASCADE
        )
    )
)
class ContactDetails (
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val userid: String,
    @ColumnInfo(name = "alias") val alias: String,
    @ColumnInfo(name = "avatar") val avatar: ByteArray,
    @ColumnInfo(name = "extra") val extra: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)