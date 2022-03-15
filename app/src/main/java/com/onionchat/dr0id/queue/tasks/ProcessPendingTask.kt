package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.MessageStatus
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.OnionTask

class ProcessPendingTask(val amountOfTasks: Int = 0, status: OnionTask.Status, exception: java.lang.Exception? = null) :
    OnionTask.Result(status, exception) {

}

class AcquirePendingTasksTask : OnionTask<ProcessPendingTask>() {
    override fun run(): ProcessPendingTask {
        // 1. check contacts state
        var sub = 0
        val usersFuture = UserManager.getAllUsers()
        val messageFuture = MessageManager.getAllWithStatus(listOf(MessageStatus.NOT_SENT.status, MessageStatus.NOT_FORWARDED.status))
        usersFuture.get().forEach {
            if (it.symaliases == null || it.symaliases!!.isEmpty()) {
                // user has no symkey... let's create the request
                enqueueDependencyTask(NegotiateSymKeyTask(it)) // we'll wait automatically... i hope
                sub += 1
            }
        }
        messageFuture.get().forEach {
            enqueueDependencyTask(ForwardMessageTask(it))
            sub += 1
        }
        return ProcessPendingTask(sub, Status.SUCCESS)
    }

    override fun onUnhandledException(exception: Exception): ProcessPendingTask {
        return ProcessPendingTask(0, Status.FAILURE, exception)
    }


}