package com.onionchat.localstorage.userstore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BroadcastDao {
    @Query("SELECT * FROM broadcast")
    fun getAll(): List<Broadcast>

    @Query("SELECT * FROM broadcast WHERE id IN (:broadcastIds)")
    fun loadAllByIds(broadcastIds: Array<String>): List<Broadcast>

//    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    fun findByName(first: String, last: String): User

    @Insert
    fun insertAll(vararg broadcasts: Broadcast)

    @Delete
    fun delete(boadcast: Broadcast)
}
