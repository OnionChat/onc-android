package com.onionchat.dr0id.messaging

class MessageParseException(message: String, content: String) : Exception("$message : <$content>") {
}