package com.onionchat.dr0id.ui.feed

import android.content.Context
import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.MessageManager.getMessagePub
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.MessageProcessor
import com.onionchat.localstorage.messagestore.EncryptedMessage
import kotlinx.atomicfu.atomic
import java.util.concurrent.Executors


class MessageLazyLoadingController(val conversation: Conversation, val context: Context) {

    enum class LAZY_LOADING_STATE {
        LOADING,
        WAITING
    }

    val defaultLimit = context.resources.getInteger(R.integer.lazy_loading_default_limit)

    val singleThreadExecutor = Executors.newSingleThreadExecutor()
    var currentOffset = 0;
    var lazyLoadingState = atomic(LAZY_LOADING_STATE.WAITING)


    fun load(offset: Int = currentOffset, limit: Int = defaultLimit, callback: (List<IMessage>) -> Unit) {
        Logging.d(TAG, "load [+] (${offset}, $limit, $callback)")

        when (lazyLoadingState.value) {
            LAZY_LOADING_STATE.LOADING -> {
                Logging.d(TAG, "load [-] already loading.. abort")
                return@load
            }
        }

        singleThreadExecutor.submit {
            var inserted = 0
            val toBeAdded = ArrayList<IMessage>()
            conversation?.let { conversation ->
                MessageManager.getRangeForConversation(conversation, offset, limit).get()?.forEach { message ->
                    if (MessageTypes.shouldBeShownInChat(message.type)) { // todo make central somehow

                        decrypt(message)?.let {
                            toBeAdded.add(it)
                            inserted += 1
                        }
                    }
//                        MessageManager.isFromMe(message)?.let {
//                            if (!it) {
//                                if (!MessageStatus.hasFlag(message.messageStatus, MessageStatus.READ)) {
//                                    sendMessageReadMessage(message)
//                                }
//                            }
//                        }
                }
            }

            currentOffset += inserted
            lazyLoadingState.lazySet(LAZY_LOADING_STATE.WAITING)
            Logging.d(TAG, "load [+] loaded <${toBeAdded.size}> messages")

            callback(toBeAdded)
        }
    }

    fun decrypt(encryptedMessage: EncryptedMessage): IMessage? {
        try {
            return MessageProcessor.unpack(encryptedMessage, getMessagePub(encryptedMessage), Crypto.getMyKey())
        } catch (e: Exception) {
            Logging.e(TAG, "decrypt [-] unable to decrypt message")
            return null
        }
    }


    companion object {
        const val TAG = "MessageLazyLoadingController"
    }
}