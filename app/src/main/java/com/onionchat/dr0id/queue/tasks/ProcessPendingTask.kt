package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Logging
import com.onionchat.common.MessageStatus
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.OnionTask

class ProcessPendingTaskResult(val amountOfTasks: Int = 0, status: OnionTask.Status, exception: java.lang.Exception? = null) :
    OnionTask.Result(status, exception) {

}

class ProcessPendingTask : OnionTask<ProcessPendingTaskResult>() {
    override fun run(): ProcessPendingTaskResult {
        // 1. check contacts state
        var sub = 0
        val usersFuture = UserManager.getAllUsers()
        val messageFuture = MessageManager.getAllWithStatus(listOf(MessageStatus.NOT_SENT.status, MessageStatus.NOT_FORWARDED.status))
        usersFuture.get().forEach {
            if (it.symaliases == null || it.symaliases!!.isEmpty()) {
                // user has no symkey... let's create the request
                Logging.d(TAG, "run [+] no symmetric key found for user $it [+] asking for a new one")
                enqueueDependencyTask(NegotiateSymKeyTask(it)) // we'll wait automatically... i hope
                sub += 1
            } else {
                Logging.d(TAG, "run [+] symmetric key found for user $it [+] ${it.symaliases!!.size}")
            }
        }
        messageFuture.get().forEach {
            enqueueDependencyTask(ForwardMessageTask(it))
//            MessageManager.deleteMessage(it)
            sub += 1
        }
        return ProcessPendingTaskResult(sub, Status.SUCCESS)
    }

    override fun onUnhandledException(exception: Exception): ProcessPendingTaskResult {
        return ProcessPendingTaskResult(0, Status.FAILURE, exception)
    }


}