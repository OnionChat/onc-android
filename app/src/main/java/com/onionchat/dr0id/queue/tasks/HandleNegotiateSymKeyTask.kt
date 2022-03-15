package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Crypto
import com.onionchat.common.IDGenerator
import com.onionchat.dr0id.database.KeyManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.keyexchange.NegotiateSymKeyMessage
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.userstore.SymAlias

class HandleNegotiateSymKeyTaskResult(
    val alias: SymAlias? = null,
    status: OnionTask.Status = OnionTask.Status.PENDING,
    exception: java.lang.Exception? = null
) :
    OnionTask.Result(status, exception) {

}

class HandleNegotiateSymKeyTask(val message: NegotiateSymKeyMessage) : OnionTask<HandleNegotiateSymKeyTaskResult>() {

    companion object {
        const val TAG = "HandleNegotiateSymKeyTask"
    }

    override fun run(): HandleNegotiateSymKeyTaskResult {
        if (message.hashedTo != IDGenerator.toHashedId(UserManager.myId!!)) {
            throw Exception("Message is not intended for my user...")
        }
        val fromUid = message.keyData.alias.userid
        if (message.hashedFrom != IDGenerator.toHashedId(fromUid)) {
            throw SecurityException("Forward negotiation messages is not supported <${message.hashedFrom}!=${IDGenerator.toHashedId(fromUid)}>")
        }
        UserManager.getUserById(message.keyData.alias.userid).get()?.let {
            if (Crypto.storeSymmetricKey(message.keyData.alias.alias, message.keyData.key) == null) {
                throw Exception("Unable to store key ${message.keyData.alias.alias}")
            }
            if (!KeyManager.addSymKey(it, message.keyData.alias).get()) {
                throw Exception("Unable to store alias $fromUid")
            }

            return HandleNegotiateSymKeyTaskResult(message.keyData.alias, Status.SUCCESS)

        } ?: run {
            throw Exception("Didn't find user in database $fromUid")
        }

    }

    override fun onUnhandledException(exception: Exception): HandleNegotiateSymKeyTaskResult {
        return HandleNegotiateSymKeyTaskResult(status = Status.FAILURE, exception = exception)
    }
}