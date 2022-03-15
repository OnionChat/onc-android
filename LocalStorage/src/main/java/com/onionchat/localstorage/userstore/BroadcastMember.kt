package com.onionchat.localstorage.userstore

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onionchat.common.IDGenerator


@Entity
data class BroadcastMember(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "broadcast_id") val broadcast_id: String,
    @ColumnInfo(name = "user_id") val user_id: String
) {

    companion object {
        fun createList(broadcast: Broadcast, users: List<User>): ArrayList<BroadcastMember> {
            val memberList = ArrayList<BroadcastMember>()
            users.forEach {
                memberList.add(BroadcastMember(IDGenerator.toHashedId(broadcast.id + it.id), broadcast.id, it.id))
            }
            return memberList
        }
    }
}