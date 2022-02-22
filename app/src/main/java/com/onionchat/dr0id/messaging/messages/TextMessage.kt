package com.onionchat.dr0id.messaging.messages

open class TextMessage(text: String, fromUser: String, signature: String = "") : Message(text.toByteArray(), fromUser, signature) { // todo user base64

    open fun getText(): String {
        return String(messageBytes)
    }
}