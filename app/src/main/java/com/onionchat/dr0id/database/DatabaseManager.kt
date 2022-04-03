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
    const val TAG = "DatabaseManager"

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
    val MIGRATION_5_6: Migration = object : Migration(5,6) {
        override fun migrate(database: SupportSQLiteDatabase) {

            database.execSQL(
                "Create table contactdetails (id TEXT PRIMARY KEY NOT NULL,userid TEXT NOT NULL, alias TEXT NOT NULL,avatar BLOB NOT NULL, extra TEXT NOT NULL, timestamp INTEGER NOT NULL, FOREIGN KEY(userid) REFERENCES User(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            database.execSQL(
                "CREATE INDEX index_ContactDetails_userid ON ContactDetails(userid)"
            )
        }
    }
    val MIGRATION_6_7: Migration = object : Migration(6,7) {
        override fun migrate(database: SupportSQLiteDatabase) {

            database.execSQL(
                "Create table messageforwardinfo (id TEXT PRIMARY KEY NOT NULL, message_id TEXT NOT NULL, user_id TEXT NOT NULL,timestamp INTEGER NOT NULL, FOREIGN KEY(message_id) REFERENCES EncryptedMessage(message_id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            database.execSQL(
                "CREATE INDEX index_MessageForwardInfo_message_id ON MessageForwardInfo(message_id)"
            )
        }
    }
    val MIGRATION_7_8: Migration = object : Migration(7,8) {
        override fun migrate(database: SupportSQLiteDatabase) {

            database.execSQL(
                "Create table messagereadinfo (id TEXT PRIMARY KEY NOT NULL, message_id TEXT NOT NULL, user_id TEXT NOT NULL,timestamp INTEGER NOT NULL, FOREIGN KEY(message_id) REFERENCES EncryptedMessage(message_id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            database.execSQL(
                "CREATE INDEX index_MessageReadInfo_message_id ON MessageReadInfo(message_id)"
            )
            database.execSQL(
                "Create table feedkey (id TEXT PRIMARY KEY NOT NULL,encrypted_key BLOB NOT NULL, alias TEXT NOT NULL, timestamp INTEGER NOT NULL)"
            )
        }
    }
    val MIGRATION_8_9: Migration = object : Migration(8,9) {
        override fun migrate(database: SupportSQLiteDatabase) {

            database.execSQL(
                "Create table pinginfo (id TEXT PRIMARY KEY NOT NULL, userId TEXT NOT NULL,  purpose TEXT NOT NULL, status INTEGER NOT NULL, version INTEGER NOT NULL, timestamp INTEGER NOT NULL, extra TEXT NOT NULL)"
            )
        }
    }
    fun initDatabase(context: Context) {
        executorService.submit {
            Logging.d(TAG, "initDatabase - call builder")
            db = Room.databaseBuilder(
                context.applicationContext,
                UsersDatabase::class.java, "contacts"
            ).addMigrations(MIGRATION_1_2)
             .addMigrations(MIGRATION_2_3)
             .addMigrations(MIGRATION_3_4)
             .addMigrations(MIGRATION_4_5)
             .addMigrations(MIGRATION_5_6)
             .addMigrations(MIGRATION_6_7)
             .addMigrations(MIGRATION_7_8)
             .addMigrations(MIGRATION_8_9)
                .build()
// https://github.com/sqlcipher/android-database-sqlcipher
            Logging.d(TAG, "initDatabase - database build has finished... finally")
        }
    }

    fun <T> submit(var1: Callable<T>?): Future<T> {
        return executorService.submit(var1)
    }

}