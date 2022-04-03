package com.onionchat.dr0id.messaging.messages

import com.onionchat.dr0id.messaging.data.Attachment

interface IAttachmentMessage {
    fun getAttachment(): Attachment
}
