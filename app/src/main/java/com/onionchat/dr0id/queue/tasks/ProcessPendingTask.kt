package com.onionchat.dr0id.queue.tasks

import android.content.Context
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.common.MessageStatus
import com.onionchat.common.SettingsManager
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.messages.RequestContactDetailsMessage
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.localstorage.userstore.PingInfo
import com.onionchat.localstorage.userstore.User
import java.util.*

class ProcessPendingTaskResult(val amountOfTasks: Int = 0, status: OnionTask.Status, exception: java.lang.Exception? = null) :
    OnionTask.Result(status, exception) {

}





class ProcessPendingTask(val user: User? = null) : OnionTask<ProcessPendingTaskResult>() {

    fun shouldRun(context: Context): Boolean {
        val diff = System.currentTimeMillis() - lastPendingTask
        val min = context.resources.getInteger(R.integer.pending_task_min_minutes_elapsed_between_tasks) * 60 * 1000
        if (diff <  min) {
            Logging.d(TAG, "run [+] last pending task is to close <$min> vs <$diff>")
            return false
        }
        lastPendingTask = System.currentTimeMillis()
        return true
    }

    override fun run(): ProcessPendingTaskResult {

        val context = context
        if (context == null) {
            Logging.e(TAG, "run [-] context is null... aborting.")
            return ProcessPendingTaskResult(0, Status.FAILURE)
        }
        Logging.d(TAG, "run [+] starting ProcessPendingTask <$user> last <$lastPendingTask>")
        if (!shouldRun(context)) {
            return ProcessPendingTaskResult(0, Status.PENDING)
        }
        // 1. check contacts state
        var sub = 0
        val usersFuture = UserManager.getAllUsers()
        val messageFuture = MessageManager.getAllWithStatus(listOf(MessageStatus.NOT_SENT.status, MessageStatus.NOT_FORWARDED.status))
        if (user == null) {
            usersFuture.get().forEach {
                sub += checkUser(it)
                ConnectionManager.pingUser(it).then { isOnline ->
                    Logging.d(TAG, "run [+] isOnline <$isOnline, $it>")
                }
            }
        } else {
            sub += checkUser(user)
        }
        messageFuture.get().forEach {
            if (user == null) {
                enqueueFollowUpTask(ForwardMessageTask(it))
            } else if (it.hashedTo == user.getHashedId()) {
                enqueueFollowUpTask(ForwardMessageTask(it))
            }
//            MessageManager.deleteMessage(it)
            sub += 1
        }

        return ProcessPendingTaskResult(sub, Status.SUCCESS)
    }

    private fun checkContactDetails(user: User): Boolean {
        Logging.d(TAG, "checkContactDetails [-] check $user, ${user.details}")
        if (user.details == null || user.details!!.isEmpty()) {
            OnionTaskProcessor.enqueue(
                SendMessageTask(
                    RequestContactDetailsMessage(
                        hashedFrom = IDGenerator.toHashedId(UserManager.myId!!),
                        hashedTo = user.getHashedId()
                    ), Conversation(user = user)
                )
            )
            return true
        } else {
            Logging.d(TAG, "checkContactDetails [+] $user has ${user.details!!.size}")
        }
        return false
    }

    private fun checkUser(user: User): Int {
        var sub = 0
        if (checkSymKey(user)) {
            sub += 1
        }
        if (checkContactDetails(user)) {
            sub += 1
        }
        return sub
    }

    private fun checkSymKey(user: User): Boolean {
        if (user.symaliases == null || user.symaliases!!.isEmpty()) {
            // user has no symkey... let's create the request
            Logging.d(OnionTask.TAG, "run [+] no symmetric key found for user $user [+] asking for a new one")
            enqueueFollowUpTask(NegotiateSymKeyTask(user)) // we'll wait automatically... i hope
            return true
        } else {
            Logging.d(OnionTask.TAG, "run [+] symmetric key found for user $user [+] ${user.symaliases!!.size}")
        }
        return false
    }

    override fun onUnhandledException(exception: Exception): ProcessPendingTaskResult {
        return ProcessPendingTaskResult(0, Status.FAILURE, exception)
    }

    companion object {
        const val TAG = "ProcessPendingTask"

        var lastPendingTask = System.currentTimeMillis()
    }
}