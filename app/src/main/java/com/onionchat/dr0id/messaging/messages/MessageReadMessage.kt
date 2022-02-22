package com.onionchat.dr0id.messaging.messages

class MessageReadMessage(val messageSignature: String, fromUser: String, signature: String = "") : Message(messageSignature.toByteArray(), fromUser, signature) {
}