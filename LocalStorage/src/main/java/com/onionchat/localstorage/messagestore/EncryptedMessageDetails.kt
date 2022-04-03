package com.onionchat.localstorage.messagestore

import androidx.room.Embedded
import androidx.room.Relation

data class EncryptedMessageDetails (
    @Embedded val encryptedMessage: EncryptedMessage,
    @Relation(parentColumn = "message_id", entityColumn = "message_id", entity = MessageForwardInfo::class) val messageForwardInformation: List<MessageForwardInfo>,
    @Relation(parentColumn = "message_id", entityColumn = "message_id", entity = MessageReadInfo::class) val messageReadInfo: List<MessageReadInfo>
)