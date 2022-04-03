package com.onionchat.localstorage.userstore

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactDetailsDao {
    @Query("SELECT * FROM contactdetails")
    fun getAll(): List<ContactDetails>

    @Query("SELECT * FROM contactdetails WHERE id IN (:contactDetailsIds)")
    fun loadAllByIds(contactDetailsIds: Array<String>): List<ContactDetails>

    @Query("SELECT * FROM contactdetails WHERE userid IN (:userId)")
    fun loadAllByUserId(userId: String): List<ContactDetails>

    @Insert
    fun insertAll(vararg symalias: ContactDetails)

    @Delete
    fun delete(symalias: ContactDetails)
}