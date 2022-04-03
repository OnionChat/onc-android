package com.onionchat.localstorage.userstore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FeedKeyDao {
    @Query("SELECT * FROM feedkey")
    fun getAll(): List<FeedKey>

    @Query("SELECT * FROM feedkey WHERE alias IN (:aliases)")
    fun loadAllByAlias(aliases: Array<String>): List<FeedKey>

    @Query("SELECT * FROM feedkey ORDER BY timestamp desc limit 1")
    fun getLastKey(): List<FeedKey>

    @Insert
    fun insertAll(vararg feedkeys: FeedKey)

    @Delete
    fun delete(feedkey: FeedKey)
}