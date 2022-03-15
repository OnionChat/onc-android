package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Crypto
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.dr0id.database.KeyManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.keyexchange.ResponseSymMessage
import com.onionchat.dr0id.messaging.keyexchange.ResponseSymMessageData
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.userstore.SymAlias
import com.onionchat.localstorage.userstore.User
import java.util.*

class RequestNegotiateSymKeyTask(val user: User) : OnionTask<RequestNegotiateSymKeyTask.NegotiateSymKeyResult>() {


    class NegotiateSymKeyResult(status: Status, exception: Exception? = null) : OnionTask.Result(status, exception) {
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
            ResponseSymMessage(IDGenerator.toHashedId(UserManager.myId!!), IDGenerator.toHashedId(user.id), data = ResponseSymMessageData(symAlias, key))

        // enque dependency
        val sendMessageResult = enqueueSubtask(SendMessageTask(responseSymMessage, UserManager.myId!!, user)).get()
        return when {
            sendMessageResult.status == Status.SUCCESS -> {
                NegotiateSymKeyResult(Status.SUCCESS)
            }
            sendMessageResult.exception == null -> { // just a connection issue
                NegotiateSymKeyResult(Status.PENDING)
            }
            else -> {
                NegotiateSymKeyResult(Status.FAILURE, sendMessageResult.exception)
            }
        }
    }

    override fun onUnhandledException(exception: java.lang.Exception): NegotiateSymKeyResult {
        return NegotiateSymKeyResult(Status.FAILURE, exception)
    }
}