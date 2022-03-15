package com.onionchat.dr0id.queue

import android.content.Context
import com.onionchat.common.Logging
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class OnionTask<K : OnionTask.Result> {

    private val subtaskExecutor: ExecutorService = Executors.newCachedThreadPool()
    private val subtasks = ArrayList<OnionFuture<Result>>()

    var context: Context? = null

    fun dispatch(context: Context?): K {
        Logging.d(TAG, "dispatch [+] $this context <$context>")
        this.context = context
        return try {
            val res = run()
            Logging.d(TAG, "dispatch [+] waiting for subtasks <${subtasks.size}>")
            subtasks.forEach {
                it.get()
            }
            res
        } catch (exception: Exception) {
            Logging.e(TAG, "dispatch [-] Error while perform task <$this>", exception)
            onUnhandledException(exception)
        }
    }


    abstract fun run(): K

    abstract fun onUnhandledException(exception: java.lang.Exception): K

    fun <K : Result> enqueueSubtask(task: OnionTask<K>): OnionFuture<K> {
        Logging.d(TAG, "enqueue [+] enqueue subtask <$task>")

        val future = OnionFuture(subtaskExecutor.submit(Callable {
            //OnionTaskProcessor.observers.forEach { it.onTaskEnqueued(task) } // todo valid?
            val res = task.dispatch(this.context)
            //OnionTaskProcessor.observers.forEach { it.onTaskFinished(task, res) }
            res
        }))
        subtasks.add(future as OnionFuture<Result>)
        return future
    }

    fun <K : Result> enqueueDependencyTask(task: OnionTask<K>) {
        Logging.d(TAG, "enqueue [+] enqueue dependency <$task>")
        OnionTaskProcessor.enqueue(task) // todo fire and forget :(
    }

    open class Result(val status: Status = Status.PENDING, val exception: java.lang.Exception?)

    enum class Status {
        SUCCESS,
        FAILURE,
        PENDING
    }

    companion object {
        const val TAG = "OnionTask"
    }
}