package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Crypto
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.KeyManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.keyexchange.NegotiateSymKeyMessage
import com.onionchat.dr0id.messaging.keyexchange.NegotiateSymKeyMessageData
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.userstore.SymAlias
import com.onionchat.localstorage.userstore.User
import java.util.*

class NegotiateSymKeyTask(val user: User) : OnionTask<NegotiateSymKeyTask.NegotiateSymKeyResult>() {


    class NegotiateSymKeyResult(val newAlias: SymAlias? = null, status: Status, exception: Exception? = null) : OnionTask.Result(status, exception) {
    }

    override fun run(): NegotiateSymKeyResult {
        // 1. generate new key
        val keyAlias = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val key = Crypto.generateSymmetricKey(keyAlias)
        Logging.d(TAG, "run [+] generated new key for user $user (alias=<$keyAlias>, key=<$key>, size=<${key.size}>)")
        if (key.isEmpty()) {
            throw Exception("Invalid key size ${key.size}")
        }
        val symAlias = SymAlias(UUID.randomUUID().toString(), user.id, keyAlias, timestamp)
        if (!KeyManager.addSymKey(user, symAlias).get()) {
            throw Exception("Unable to store alias $symAlias")
        }

        // create message
        val responseSymMessage =
            NegotiateSymKeyMessage(
                IDGenerator.toHashedId(UserManager.myId!!),
                IDGenerator.toHashedId(user.id),
                keyData = NegotiateSymKeyMessageData(symAlias, key)
            )

        // enque dependency
        val sendMessageResult = enqueueSubtask(SendMessageTask(responseSymMessage, Conversation(user = user))).get()
        return when {
            sendMessageResult.status == Status.SUCCESS -> {
                NegotiateSymKeyResult(symAlias, Status.SUCCESS)
            }
            sendMessageResult.exception == null -> { // just a connection issue
                NegotiateSymKeyResult(status = Status.PENDING)
            }
            else -> {
                NegotiateSymKeyResult(status = Status.FAILURE, exception = sendMessageResult.exception)
            }
        }
    }

    override fun onUnhandledException(exception: java.lang.Exception): NegotiateSymKeyResult {
        return NegotiateSymKeyResult(status = Status.FAILURE, exception = exception)
    }

    companion object {
        const val TAG = "NegotiateSymKeyTask"
    }
}