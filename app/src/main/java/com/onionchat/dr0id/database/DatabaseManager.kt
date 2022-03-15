package com.onionchat.dr0id.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.onionchat.common.Logging
import com.onionchat.localstorage.userstore.UsersDatabase
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

object DatabaseManager {
    lateinit var db: UsersDatabase;

    var executorService = Executors.newFixedThreadPool(1)

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "Create table broadcast (id TEXT PRIMARY KEY NOT NULL, label TEXT NOT NULL)"
            )
        }
    }
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "Create table broadcastmember (id TEXT PRIMARY KEY NOT NULL, broadcast_id TEXT NOT NULL, user_id TEXT NOT NULL)"
            )
            database.execSQL(
                "Create table blockedconversation (id TEXT PRIMARY KEY NOT NULL, conversation_id TEXT NOT NULL)"
            )
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "Create table encryptedmessage (message_id TEXT PRIMARY KEY NOT NULL,message_data BLOB NOT NULL, hashed_from TEXT NOT NULL, hashed_to TEXT NOT NULL, signature TEXT NOT NULL, status INTEGER NOT NULL, read INTEGER NOT NULL, type INTEGER NOT NULL, created INTEGER NOT NULL, extra TEXT NOT NULL)"
            )
        }
    }
    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {

            database.execSQL(
                "Create table symalias (id TEXT PRIMARY KEY NOT NULL,userid TEXT NOT NULL, alias TEXT NOT NULL, timestamp INTEGER NOT NULL, FOREIGN KEY(userid) REFERENCES User(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            database.execSQL(
                "CREATE INDEX index_SymAlias_userid ON Symalias(userid)"
            )
        }
    }
    fun initDatabase(context: Context) {
        executorService.submit {
            Logging.d("UserManager", "initDatabase - call builder")
            db = Room.databaseBuilder(
                context.applicationContext,
                UsersDatabase::class.java, "contacts"
            ).addMigrations(MIGRATION_1_2)
             .addMigrations(MIGRATION_2_3)
             .addMigrations(MIGRATION_3_4)
             .addMigrations(MIGRATION_4_5)
                .build()
            Logging.d("UserManager", "initDatabase - check dummies")
            /*if (getAllUsers().get().isEmpty()) {
                Logging.log("UserManager", "initDatabase - user dummy data")
                addUser(User("25eb3d9ec4a6019c", "1"))
                addUser(User("893190354fb2c072", "2"))
            }*/
//            addUser(User("7rksqbjmgbvhxzmed5vz7wey5s4ajpwmnh6dvmmaf22pwyhe7djw7had.onion", "3"))
//            addUser(User("nlblw3jxzwkxaug7q3pekpqqudg4qvvpzc7lljeno52qzaihu2cw63yd.onion", "2"))
            Logging.d("UserManager", "initDatabase - database build has finished... finally")
        }
    }

    fun <T> submit(var1: Callable<T>?): Future<T> {
        return executorService.submit(var1)
    }

}