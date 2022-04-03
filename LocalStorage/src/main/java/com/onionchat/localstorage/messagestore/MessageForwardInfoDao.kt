package com.onionchat.localstorage.messagestore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.onionchat.localstorage.userstore.SymAlias

@Dao
interface MessageForwardInfoDao {
    @Query("SELECT * FROM messageforwardinfo")
    fun getAll(): List<MessageForwardInfo>

    @Query("SELECT * FROM messageforwardinfo WHERE message_id IN (:messageIds)")
    fun loadAllByMessageIds(messageIds: List<String>): List<MessageForwardInfo>

    @Query("SELECT * FROM messageforwardinfo WHERE user_id IN (:userId)")
    fun loadAllByUserId(userId: String): List<MessageForwardInfo>

    @Insert
    fun insertAll(vararg messageForwardInfo: MessageForwardInfo)

    @Insert
    fun insertAll(messageForwardInfo: List<MessageForwardInfo>)

    @Delete
    fun delete(messageForwardInfo: MessageForwardInfo)
}