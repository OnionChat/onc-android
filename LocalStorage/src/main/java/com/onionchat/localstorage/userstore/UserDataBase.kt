package com.onionchat.localstorage.userstore

import androidx.room.Database
import androidx.room.RoomDatabase
import com.onionchat.localstorage.messagestore.*

@Database(
    entities = [User::class,
        Broadcast::class,
        BroadcastMember::class,
        BlockedConversation::class,
        EncryptedMessage::class,
        SymAlias::class,
        ContactDetails::class,
        MessageForwardInfo::class,
        MessageReadInfo::class,
        FeedKey::class,
        PingInfo::class], version = 9
)
abstract class UsersDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun broadcastDao(): BroadcastDao
    abstract fun broadcastMemberDao(): BroadcastMemberDao
    abstract fun blockedConversationDao(): BlockedConversationDao
    abstract fun messageDao(): EncryptedMessageDao
    abstract fun symAliasDao(): SymAliasDao
    abstract fun contactDetailsDao(): ContactDetailsDao
    abstract fun messageForwardInfoDao(): MessageForwardInfoDao
    abstract fun messageReadInfoDao(): MessageReadInfoDao
    abstract fun feedKeyDao(): FeedKeyDao
    abstract fun pingInfoDao(): PingInfoDao

}
