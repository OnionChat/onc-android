package com.onionchat.localstorage.userstore

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [User::class, Broadcast::class], version = 2)
abstract class UsersDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun broadcastDao(): BroadcastDao
}
