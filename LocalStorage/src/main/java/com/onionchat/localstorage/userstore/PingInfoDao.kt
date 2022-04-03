package com.onionchat.localstorage.userstore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query


@Dao
interface PingInfoDao {
    @Query("SELECT * FROM pinginfo")
    fun getAll(): List<PingInfo>

    @Query("SELECT * FROM pinginfo WHERE id IN (:pingInfoIds)")
    fun loadAllByIds(pingInfoIds: Array<String>): List<PingInfo>

    @Query("SELECT * FROM pinginfo WHERE userid IN (:userId) ORDER BY timestamp asc")
    fun loadAllByUserId(userId: String): List<PingInfo>

    @Query("SELECT * FROM pinginfo WHERE userid IN (:userId) AND status IS (:status) ORDER BY timestamp desc limit 1")
    fun loadLatestByUserIdAndStatus(userId: String, status: Int): List<PingInfo>

    @Query("SELECT * FROM pinginfo WHERE userid IN (:userId) AND timestamp > (:startTimeMillis)")
    fun loadTimeRangeByUserId(userId: String, startTimeMillis: Long): List<PingInfo>

    @Insert
    fun insertAll(vararg pinginfo: PingInfo)

    @Delete
    fun delete(pinginfo: PingInfo)
}