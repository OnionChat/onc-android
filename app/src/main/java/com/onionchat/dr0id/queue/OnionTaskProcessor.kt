package com.onionchat.dr0id.queue

import android.content.Context
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.Executors

object OnionTaskProcessor { // todo make content provider ?

    private var context: WeakReference<Context?>? = null

    val executorService = Executors.newFixedThreadPool(4)

    val observers = ArrayList<OnionTaskProcessorObserver>()
    fun addObserver(observer: OnionTaskProcessorObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: OnionTaskProcessorObserver) {
        observers.remove(observer)
    }

    fun attachContext(context: Context) {
        this.context = WeakReference(context)
    }

    fun <K : OnionTask.Result> enqueue(task: OnionTask<K>): OnionFuture<K> {
        return OnionFuture(executorService.submit(Callable {
            observers.forEach { it.onTaskEnqueued(task) } // todo valid?
            val res = task.dispatch(context?.get())
            observers.forEach { it.onTaskFinished(task, res) }
            res
        }))
    }


    interface OnionTaskProcessorObserver {
        fun onTaskEnqueued(task: Any)
        fun onTaskFinished(task: Any, result: OnionTask.Result)
    }

}