package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Crypto
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.messages.ProvideContactDetailsMessage
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.EncryptedLocalStorage
import com.onionchat.localstorage.userstore.ContactDetails
import com.onionchat.localstorage.userstore.User
import java.util.*

class ProvideContactDetailsTask(val to: User) : OnionTask<ProvideContactDetailsTask.ProvideContactDetailsResult>() {


    class ProvideContactDetailsResult(status: Status, exception: Exception? = null) : OnionTask.Result(status, exception) {
    }

    override fun run(): ProvideContactDetailsResult {
        // 1. build contact details
        val context = context
        if (context == null) {
            Logging.e(TAG, "run [-] context is null!! Abort.")
            return ProvideContactDetailsResult(Status.FAILURE)
        }
        val storage = EncryptedLocalStorage(Crypto.getMyPublicKey()!!, Crypto.getMyKey(), context)
        var label = storage.getValue(context.getString(R.string.key_user_label))
        if (label == null) {
            Logging.e(TAG, "run [-] unable to retrieve label")
            label = ""
        }
        val contactDetails = ContactDetails(
            UUID.randomUUID().toString(),
            UserManager.myId!!,
            label,
            byteArrayOf(),
            "",
            System.currentTimeMillis()
        ) // todo store my contact details too

        // 2. send contact details
        enqueueFollowUpTask(
            SendMessageTask(
                ProvideContactDetailsMessage(
                    contactDetails,
                    hashedFrom = IDGenerator.toHashedId(UserManager.myId!!),
                    hashedTo = to.getHashedId()
                ),
                Conversation(user = to)
            )
        )
        return ProvideContactDetailsResult(Status.SUCCESS)
    }

    override fun onUnhandledException(exception: java.lang.Exception): ProvideContactDetailsResult {
        return ProvideContactDetailsResult(status = Status.FAILURE, exception = exception)
    }

    companion object {
        const val TAG = "ProvideContactDetailsTask"
    }
}