package com.onionchat.localstorage.userstore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BlockedConversationDao {
    @Query("SELECT * FROM broadcastmember")
    fun getAll(): List<BroadcastMember>

    @Query("SELECT * FROM broadcastmember WHERE broadcast_id IN (:broadcastIds)")
    fun loadAllByIds(broadcastIds: Array<String>): List<BroadcastMember>

//    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    fun findByName(first: String, last: String): User

    @Insert
    fun insertAll(vararg broadcastMember: BroadcastMember)

    @Delete
    fun delete(broadcastMember: BroadcastMember)
}
