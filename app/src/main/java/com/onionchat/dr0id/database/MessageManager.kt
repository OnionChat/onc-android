package com.onionchat.dr0id.database

import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.common.MessageStatus
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.userstore.Conversation
import java.util.concurrent.Future

object MessageManager {

    const val TAG = "MessageManager"

    fun storeEncryptedMessage(encryptedMessage: EncryptedMessage): Future<Boolean> {
        return DatabaseManager.submit {
            DatabaseManager.db.messageDao().insertAll(encryptedMessage)
            true
        }
    }

    fun updateEncryptedMessage(encryptedMessage: EncryptedMessage): Future<Boolean> {
        return DatabaseManager.submit {
            DatabaseManager.db.messageDao().update(encryptedMessage)
            true
        }
    }

    fun getAllWithStatus(stati: List<Int>): Future<List<EncryptedMessage>> {
        return DatabaseManager.submit {
            DatabaseManager.db.messageDao().loadAllByStatus(stati) // change sql !!
        }
    }

    fun getAllForConversation(conversation: Conversation): Future<List<EncryptedMessage>> {
        return DatabaseManager.submit {
            if (conversation.user != null) {
                return@submit DatabaseManager.db.messageDao().loadAllByHashedUserIds(arrayOf(IDGenerator.toHashedId(conversation.user!!.id))) // change sql !!
            } else {
                listOf()
            }
        }
    }

    fun deleteMessage(message: EncryptedMessage): Future<Unit> {
        return DatabaseManager.submit {
            DatabaseManager.db.messageDao().delete(message)
        }
    }

    fun setMessageRead(messageSignature: String): Future<Int> {
        return DatabaseManager.submit {
            var updated = 0
            DatabaseManager.db.messageDao().loadMessagesBySignaure(arrayOf(messageSignature)).forEach {
                Logging.d(TAG, "setMessageRead [+] update message <${it.messageId}")
                it.messageStatus = MessageStatus.addFlag(it.messageStatus, MessageStatus.READ)
                updateEncryptedMessage(it)
                updated += 1
            }
            Logging.d(TAG, "setMessageRead [+] updated messages <$updated>")

            updated
        }
    }
}