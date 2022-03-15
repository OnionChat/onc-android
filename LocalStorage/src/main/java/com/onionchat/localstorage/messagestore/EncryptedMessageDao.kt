package com.onionchat.localstorage.messagestore

import androidx.room.*

@Dao
interface EncryptedMessageDao {
    @Query("SELECT * FROM encryptedmessage")
    fun getAll(): List<EncryptedMessage>

    @Query("SELECT * FROM encryptedmessage WHERE hashed_from IN (:hashedUserIds) OR hashed_to IN (:hashedUserIds)")
    fun loadAllByHashedUserIds(hashedUserIds: Array<String>): List<EncryptedMessage>

    @Query("SELECT * FROM encryptedmessage WHERE status IN (:stati)")
    fun loadAllByStatus(stati:List<Int>): List<EncryptedMessage>

    @Query("SELECT * FROM encryptedmessage WHERE signature IN (:signatures)")
    fun loadMessagesBySignaure(signatures: Array<String>): List<EncryptedMessage>

//    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    fun findByName(first: String, last: String): User

    @Insert
    fun insertAll(vararg encryptedMessage: EncryptedMessage)

    @Insert
    fun insertAllMessages(encrytedMessages: Array<EncryptedMessage>)

    @Update
    fun update(encryptedMessage: EncryptedMessage)

    @Delete
    fun delete(encryptedMessage: EncryptedMessage)

    @Query("DELETE FROM encryptedmessage WHERE message_id IN (:messageIds)")
    fun deleteMessagesById(messageIds: Array<String>)
}
