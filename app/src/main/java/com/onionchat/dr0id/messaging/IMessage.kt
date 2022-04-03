package com.onionchat.dr0id.messaging

import com.onionchat.common.DateTimeHelper
import com.onionchat.localstorage.messagestore.EncryptedMessage
import java.text.SimpleDateFormat
import java.util.*

abstract class IMessage(    val messageId: String = UUID.randomUUID().toString(),
                            var hashedFrom: String,
                            var hashedTo: String,
                            var signature: String = "",
                            var messageStatus: Int = 0,
                            var created: Long = System.currentTimeMillis(),
                            var read: Int = 0,
                            var type: Int,
                            var extra: String = "") {

    abstract fun getMessageType(): Int

    fun getCreationTimeText(): String {
        return DateTimeHelper.timestampToTimeString(created)
    }

    fun getCreationDateText(): String {
        return DateTimeHelper.timestampToDateString(created)
    }

    override fun toString(): String {
        return "IMessage(messageId='$messageId', hashedFrom='$hashedFrom', hashedTo='$hashedTo', signature='$signature', messageStatus=$messageStatus, created=$created, read=$read, type=$type, extra='$extra')"
    }

    companion object {
        const val EXTRA_QUOTED_MESSAGE_ID = "quoted_message_id"
    }



}