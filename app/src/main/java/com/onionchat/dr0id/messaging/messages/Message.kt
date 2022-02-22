package com.onionchat.dr0id.messaging.messages

import com.onionchat.connector.Communicator

open class Message(val messageBytes: ByteArray, val from: String, var signature: String = "", var read: Boolean = false, var status: Communicator.MessageSentStatus =  Communicator.MessageSentStatus.ONGIONG) {


}