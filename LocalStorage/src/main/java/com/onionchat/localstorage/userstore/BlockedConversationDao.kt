package com.onionchat.localstorage.userstore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BlockedConversationDao {
    @Query("SELECT * FROM blockedconversation")
    fun getAll(): List<BlockedConversation>

    @Query("SELECT * FROM blockedconversation WHERE conversation_id IN (:conversation_ids)")
    fun loadAllByConversationIds(conversation_ids: Array<String>): List<BlockedConversation>

//    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    fun findByName(first: String, last: String): User

    @Insert
    fun insertAll(vararg blockedConversation: BlockedConversation)

    @Delete
    fun delete(blockedConversation: BlockedConversation)
}
