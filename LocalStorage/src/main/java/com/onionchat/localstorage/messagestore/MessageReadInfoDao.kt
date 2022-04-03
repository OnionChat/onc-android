package com.onionchat.localstorage.messagestore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query


@Dao
interface MessageReadInfoDao {
    @Query("SELECT * FROM messagereadinfo")
    fun getAll(): List<MessageReadInfo>

    @Query("SELECT * FROM messagereadinfo WHERE message_id IN (:messageIds)")
    fun loadAllByMessageIds(messageIds: List<String>): List<MessageForwardInfo>

    @Query("SELECT * FROM messagereadinfo WHERE user_id IN (:userId)")
    fun loadAllByUserId(userId: String): List<MessageForwardInfo>

    @Insert
    fun insertAll(vararg messageReadInfo: MessageReadInfo)

    @Insert
    fun insertAll(messageReadInfo:List<MessageReadInfo>)

    @Delete
    fun delete(messageReadInfo: MessageReadInfo)
}