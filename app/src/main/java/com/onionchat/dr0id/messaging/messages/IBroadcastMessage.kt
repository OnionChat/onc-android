package com.onionchat.dr0id.messaging.messages

import com.onionchat.localstorage.userstore.Broadcast

interface IBroadcastMessage {
    fun getBroadcast(): Broadcast
}