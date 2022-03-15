package com.onionchat.localstorage.userstore

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onionchat.common.IDGenerator

@Entity
data class Broadcast(@PrimaryKey val id: String, @ColumnInfo(name = "label") val label: String) {

    companion object {
        fun generateId(label: String, uid: String): String {
            return IDGenerator.toHashedId(label + uid)
        }
    }
}