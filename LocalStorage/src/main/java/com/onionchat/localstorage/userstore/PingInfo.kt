package com.onionchat.localstorage.userstore

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity
data class PingInfo(@PrimaryKey val id: String,/* hashed user id*/ @ColumnInfo(name = "userId") val userId: String,  @ColumnInfo(name = "purpose") val purpose: String, @ColumnInfo(name = "status") val status: Int,  @ColumnInfo(name = "version") val version: Int,  @ColumnInfo(name = "timestamp") val timestamp: Long, @ColumnInfo(name = "extra") val extra: String) {

    enum class PingInfoStatus {
        SUCCESS,
        FAILURE,
        RECEIVED
    }
}