package com.onionchat.localstorage.messagestore

import androidx.room.*
import com.onionchat.localstorage.userstore.UserJoined

@Dao
interface EncryptedMessageDao {
    @Query("SELECT * FROM encryptedmessage")
    fun getAll(): List<EncryptedMessage>

    @Query("SELECT * FROM encryptedmessage WHERE hashed_from IN (:hashedUserIds) OR hashed_to IN (:hashedUserIds) ORDER BY created")
    fun loadAllByHashedUserIds(hashedUserIds: Array<String>): List<EncryptedMessage>

    @Query("SELECT * FROM encryptedmessage WHERE type IN (:types) AND ((hashed_from IN (:hashedUserIds) AND hashed_to IS (:myId)) OR (hashed_to IN (:hashedUserIds)) AND hashed_from IS (:myId)) ORDER BY created desc limit (:limit) offset (:offset)")
    fun loadRangeByHashedUserIds(hashedUserIds: Array<String>, myId:String, offset:Int, limit:Int, types: List<Int>): List<EncryptedMessage>

    @Query("SELECT * FROM encryptedmessage WHERE type IN (:types) AND (hashed_from IN (:hashedUserIds) OR hashed_to IN (:hashedUserIds)) ORDER BY created desc limit (:limit) offset (:offset)")
    fun loadRangeByHashedUserIds(hashedUserIds: Array<String>,  offset:Int, limit:Int, types: List<Int>): List<EncryptedMessage>

    @Query("SELECT * FROM encryptedmessage WHERE status IN (:stati)")
    fun loadAllByStatus(stati:List<Int>): List<EncryptedMessage>

    @Query("SELECT * FROM encryptedmessage WHERE signature IN (:signatures)")
    fun loadMessagesBySignaure(signatures: Array<String>): List<EncryptedMessage>

    @Query("SELECT * FROM encryptedmessage WHERE message_id IN (:messageIds)")
    fun loadMessageById(messageIds: Array<String>): List<EncryptedMessage>

    @Query("SELECT * FROM encryptedmessage WHERE type IN (:types) AND ((hashed_from IS (:hashedUserId) AND hashed_to IS (:myId)) OR (hashed_to IS (:hashedUserId)) AND hashed_from IS (:myId)) ORDER BY created desc limit 1")
    fun loadLastMessage(hashedUserId: String, myId:String, types:List<Int>): List<EncryptedMessage>

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

    @Query("SELECT * FROM encryptedmessage WHERE message_id IN (:messageIds)")
    abstract fun getDetails(messageIds: Array<String>): EncryptedMessageDetails
}
