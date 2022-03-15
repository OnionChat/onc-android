package com.onionchat.localstorage.userstore

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.messagestore.EncryptedMessageDao

@Database(entities = [User::class, Broadcast::class, BroadcastMember::class, BlockedConversation::class, EncryptedMessage::class, SymAlias::class], version = 5)
abstract class UsersDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun broadcastDao(): BroadcastDao
    abstract fun broadcastMemberDao(): BroadcastMemberDao
    abstract fun blockedConversationDao(): BlockedConversationDao
    abstract fun messageDao(): EncryptedMessageDao
    abstract fun symAliasDao() : SymAliasDao
}
