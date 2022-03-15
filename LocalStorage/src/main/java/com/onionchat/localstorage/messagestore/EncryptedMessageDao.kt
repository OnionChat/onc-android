package com.onionchat.localstorage.messagestore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.onionchat.localstorage.userstore.BroadcastMember

@Dao
interface EncryptedMessageDao {
    @Query("SELECT * FROM encryptedmessage")
    fun getAll(): List<BroadcastMember>

    @Query("SELECT * FROM broadcastmember WHERE broadcast_id IN (:broadcastIds)")
    fun loadAllByBroadcastIds(broadcastIds: Array<String>): List<BroadcastMember>

//    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    fun findByName(first: String, last: String): User

    @Insert
    fun insertAll(vararg broadcastMember: BroadcastMember)

    @Insert
    fun insertAllMembers(broadcastMember: Array<BroadcastMember>)

    @Delete
    fun delete(broadcastMember: BroadcastMember)

    @Query("DELETE FROM broadcastmember WHERE broadcast_id = (:broadcastId) AND user_id IN (:userIds)")
    fun deleteMembersOfBroadcast(broadcastId: String, userIds :Array<String>)
}
