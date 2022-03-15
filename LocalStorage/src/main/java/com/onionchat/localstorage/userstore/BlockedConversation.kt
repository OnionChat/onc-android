package com.onionchat.localstorage.userstore

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BlockedConversation(@PrimaryKey val id: String, @ColumnInfo(name = "conversation_id") val conversation_id: String)
