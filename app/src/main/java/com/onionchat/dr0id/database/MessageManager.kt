package com.onionchat.dr0id.database

import com.onionchat.common.*
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.messages.MessageReadMessage
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.messagestore.EncryptedMessageDetails
import com.onionchat.localstorage.messagestore.MessageForwardInfo
import com.onionchat.localstorage.messagestore.MessageReadInfo
import com.onionchat.localstorage.userstore.User
import java.security.cert.Certificate
import java.util.*
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

    fun getRangeForConversation(
        conversation: Conversation,
        offset: Int,
        limit: Int,
        types: List<Int> = MessageTypes.renderableMessages()
    ): Future<List<EncryptedMessage>> {
        return DatabaseManager.submit {
            try {
                Logging.d(TAG, "getRangeForConversation [+] <$conversation, ${conversation.getConversationType()}>")
                when (conversation.getConversationType()) {
                    ConversationType.CHAT -> {

                        val myHashedId = UserManager.getMyHashedId()
                        if (conversation.user != null) {
                            return@submit DatabaseManager.db.messageDao()
                                .loadRangeByHashedUserIds(
                                    arrayOf(conversation.getHashedId()),
                                    myHashedId,
                                    offset,
                                    limit,
                                    types
                                ) // change sql !!
                        } else {
                            Logging.e(TAG, "getRangeForConversation [-] unexpected error (user is null)")
                            listOf()
                        }
                    }
                    ConversationType.BROADCAST -> {

                        if (conversation.broadcast != null) {
                            return@submit DatabaseManager.db.messageDao()
                                .loadRangeByHashedUserIds(arrayOf(conversation.getHashedId()), offset, limit, types) // change sql !!
                        } else {
                            Logging.e(TAG, "getRangeForConversation [-] unexpected error (broadcast is null)")
                            listOf()
                        }
                    }
                    ConversationType.FEED -> {

                        if (conversation.feedId != null) {
                            return@submit DatabaseManager.db.messageDao()
                                .loadRangeByHashedUserIds(arrayOf(conversation.getHashedId()), offset, limit, types) // change sql !!
                        } else {
                            Logging.e(TAG, "getRangeForConversation [-] unexpected error (feedId is null)")
                            listOf()
                        }
                    }
                    else -> {
                        Logging.e(TAG, "getRangeForConversation [-] unsupporeted conversation type $conversation")
                        listOf()
                    }
                }
            } catch (exception: Exception) {
                Logging.e(TAG, "getRangeForConversation [-] error while retrieve messages", exception)
                listOf()
            }
        }
    }

    fun getLastMessage(user: User, types: List<Int> = MessageTypes.renderableMessages()): Future<EncryptedMessage?> {
        return DatabaseManager.submit {
            try {
                val myId = UserManager.myId
                if (myId == null) {
                    Logging.e(TAG, "getLastMessage [-] my id is null :(")
                    return@submit null
                }
                val myHashedId = IDGenerator.toHashedId(myId)
                val messages = DatabaseManager.db.messageDao().loadLastMessage(IDGenerator.toHashedId(user.id), myHashedId, types) // change sql !!
                if (messages.isEmpty()) {
                    null
                } else {
                    messages[0]
                }
            } catch (exception: Exception) {
                Logging.e(TAG, "error while get last message [-] message", exception)
                null
            }
        }
    }

    fun deleteMessage(message: EncryptedMessage): Future<Unit> {
        return DatabaseManager.submit {
            DatabaseManager.db.messageDao().delete(message)
        }
    }

    fun setMessageRead(messageReadMessage: MessageReadMessage): Future<Int> {
        return DatabaseManager.submit {
            var updated = 0
            DatabaseManager.db.messageDao().loadMessagesBySignaure(arrayOf(messageReadMessage.messageSignature)).forEach {
                Logging.d(TAG, "setMessageRead [+] update message <${it.messageId}, ${MessageStatus.dumpState(it.messageStatus)}>")
                it.messageStatus = MessageStatus.addFlag(it.messageStatus, MessageStatus.READ)
                updateEncryptedMessage(it)
                Logging.d(TAG, "setMessageRead [+] updated message <${it.messageId}, ${MessageStatus.dumpState(it.messageStatus)}>")

                DatabaseManager.db.messageReadInfoDao().insertAll(
                    MessageReadInfo(
                        UUID.randomUUID().toString(),
                        it.messageId,
                        messageReadMessage.hashedFrom,
                        messageReadMessage.created
                    )
                )
                updated += 1
            }
            Logging.d(TAG, "setMessageRead [+] updated messages <$updated>")

            updated
        }
    }

    fun getMessageById(messageId: String): Future<EncryptedMessage?> {
        return DatabaseManager.submit {

            Logging.d(TAG, "getMessageById [+] retrieve message by id <$messageId>")

            val messages = DatabaseManager.db.messageDao().loadMessageById(arrayOf(messageId))
            if (messages.isNotEmpty()) {
                if (messages.size > 1) {
                    Logging.e(TAG, "getMessageById [+] got multiple messages for id <$messageId> => <${messages.size}>")
                }
                return@submit messages[0]
            }
            null
        }
    }

    fun getMessagePub(encryptedMessage: EncryptedMessage): Certificate? {
        return if (encryptedMessage.hashedFrom == UserManager.getMyHashedId()) {
            Crypto.getMyPublicKey()
        } else {
            val user = UserManager.getUserByHashedId(encryptedMessage.hashedFrom).get() // todo async ?
            if (user == null) {
                Logging.e(TAG, "getMessagePub [-] User <${encryptedMessage.hashedFrom}> not found")
                return null
            }
            val cert = Crypto.getPublicKey(user.certId)
            if (cert == null) {
                Logging.e(TAG, "getMessagePub [-] Cannot load certificate <${user.certId}>")
                return null
            }
            cert
        }
    }
    fun getMessagePubByHashedUserId(hashedFrom:String): Certificate? {
        return if (hashedFrom == UserManager.getMyHashedId()) {
            Crypto.getMyPublicKey()
        } else {
            val user = UserManager.getUserByHashedId(hashedFrom).get() // todo async ?
            if (user == null) {
                Logging.e(TAG, "User <${hashedFrom}> not found")
                return null
            }
            val cert = Crypto.getPublicKey(user.certId)
            if (cert == null) {
                Logging.e(TAG, "Cannot load certificate <${user.certId}>")
                return null
            }
            cert
        }
    }


    fun getMessagePub(partner: User, encryptedMessage: EncryptedMessage): Certificate? {
        var pub = Crypto.getPublicKey(partner.certId)
        if (encryptedMessage.hashedFrom.equals(IDGenerator.toHashedId(UserManager.myId!!))) {
            pub = Crypto.getMyPublicKey()
        }
        return pub!! // todo fix me
    }

    fun isFromMe(message: IMessage): Boolean? {
        return isFromMe(message.hashedFrom)
    }
    fun isFromMe(message: EncryptedMessage): Boolean? {
        return isFromMe(message.hashedFrom)
    }
    fun isFromMe(hashedFrom : String): Boolean? {
        UserManager.myId?.let {
            return@isFromMe IDGenerator.toHashedId(it) == hashedFrom
        }
        return null
    }

    fun getEncryptedMessageDetails(messageId: String): Future<EncryptedMessageDetails?> {
        return DatabaseManager.submit {
            try {
                val messageDetails = DatabaseManager.db.messageDao().getDetails(arrayOf(messageId))
                return@submit messageDetails
            } catch (exception: Exception) {
                Logging.e(TAG, "getEncryptedMessageDetails [-] error while message forward info <$messageId>", exception)
                null
            }
        }
    }

    fun insertForwardInformation(forwardInfo: List<MessageForwardInfo>): Future<Any> {
        return DatabaseManager.submit {
            try {
                DatabaseManager.db.messageForwardInfoDao().insertAll(forwardInfo)
            } catch (exception: Exception) {
                Logging.e(TAG, "insertForwardInformation [-] error while insert forward info <${forwardInfo.count()}>", exception)
            }
        }
    }
}