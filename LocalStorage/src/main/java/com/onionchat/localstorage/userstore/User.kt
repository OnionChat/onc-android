package com.onionchat.localstorage.userstore

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.onionchat.common.IDGenerator

@Entity
data class User(@PrimaryKey val id: String, @ColumnInfo(name = "cert_id") val certId: String) {

    @Ignore
    var symaliases: List<SymAlias>? = null

    @Ignore
    var details: List<ContactDetails>? = null

    fun getHashedId(): String {
        return IDGenerator.toHashedId(id)
    }

    fun getLastAlias(): ContactDetails? {
        return details?.maxByOrNull { it.timestamp } // todo check if timestamp sort is correct
    }
    fun getLastSymAlias(): SymAlias? {
        return symaliases?.maxByOrNull { it.timestamp } // todo check if timestamp sort is correct
    }
}