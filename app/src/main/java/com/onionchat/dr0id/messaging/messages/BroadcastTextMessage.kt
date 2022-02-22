package com.onionchat.dr0id.messaging.messages

import org.json.JSONObject

class BroadcastTextMessage(internal val broadcastId: String, val broadcastLabel: String, val message: String, fromUser: String, signature: String = "") :
    TextMessage(toMessageString(broadcastId, broadcastLabel, message), fromUser, signature), IBroadcastMessage {

    override fun getText(): String {
        return message
    }

    companion object {
        val BROADCAST_ID = "broadcast_id"
        val BROADCAST_LABEL = "broadcast_label"
        val MESSAGE = "broadcast_message"

        fun createFromMessageString(msg: String, fromUser: String, signature: String = ""): BroadcastTextMessage? {
            val root = JSONObject(msg)
            val broadcastId = root.getString(BROADCAST_ID)!!
            val broadcastLabel = root.getString(BROADCAST_LABEL)!!
            val text = root.getString(MESSAGE)!!
            if (broadcastId.length == 0 || broadcastLabel.length == 0) {
                return null
            } else {
                return BroadcastTextMessage(broadcastId, broadcastLabel, text, fromUser, signature)
            }
        }

        fun toMessageString(broadcastId: String, broadcastLabel: String, text: String): String {
            val root = JSONObject()
            root.put(BROADCAST_ID, broadcastId)
            root.put(BROADCAST_LABEL, broadcastLabel)
            root.put(MESSAGE, text)
            return root.toString()
        }
    }

    override fun getBroadcastId(): String {
        return broadcastId
    }

}