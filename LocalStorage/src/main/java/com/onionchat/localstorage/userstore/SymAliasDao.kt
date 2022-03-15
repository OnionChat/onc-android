package com.onionchat.localstorage.userstore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query


@Dao
interface SymAliasDao {
    @Query("SELECT * FROM symalias")
    fun getAll(): List<SymAlias>

    @Query("SELECT * FROM symalias WHERE id IN (:symAliasIds)")
    fun loadAllByIds(symAliasIds: Array<String>): List<SymAlias>

    @Query("SELECT * FROM symalias WHERE userid IN (:userId)")
    fun loadAllByUserId(userId: String): List<SymAlias>

    @Insert
    fun insertAll(vararg symalias: SymAlias)

    @Delete
    fun delete(symalias: SymAlias)
}