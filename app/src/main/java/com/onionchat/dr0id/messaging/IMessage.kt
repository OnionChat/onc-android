package com.onionchat.dr0id.messaging

import com.onionchat.localstorage.messagestore.EncryptedMessage

abstract class IMessage(    val messageId: String = java.util.UUID.randomUUID().toString(),
                            var hashedFrom: String,
                            var hashedTo: String,
                            var signature: String = "",
                            var messageStatus: Int = 0,
                            var created: Long = System.currentTimeMillis(),
                            var read: Int = 0,
                            var type: Int,
                            var extra: String = "") {

    abstract fun getMessageType(): Int

}