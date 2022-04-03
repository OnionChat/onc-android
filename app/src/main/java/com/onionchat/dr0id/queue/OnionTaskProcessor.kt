package com.onionchat.dr0id.queue

import android.content.Context
import com.onionchat.common.Logging
import com.onionchat.dr0id.queue.tasks.CheckConnectionTask
import java.lang.ref.WeakReference
import java.util.concurrent.*

object OnionTaskProcessor { // todo make content provider ?

    const val TAG = "OnionTaskProcessor"
    private var context: WeakReference<Context?>? = null

    val executorService = Executors.newFixedThreadPool(16)
    val executorServiceHighPriority = Executors.newFixedThreadPool(24)

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

    fun <K : OnionTask.Result> enqueue(task: OnionTask<K>, executor: ExecutorService = executorService): OnionFuture<K> {
        if (executor is ThreadPoolExecutor) {
            Logging.d(
                TAG,
                "enqueue [+] >>>>>>>>>>>>>>> [+] task <${task}, ${task.id}> queue information: <tasksCount=${executor.taskCount}, active=${executor.activeCount}, completed=${executor.completedTaskCount}, poolsize=${executor.maximumPoolSize}>"
            )
        }
        return OnionFuture(executor, executor.submit(Callable {
            observers.forEach { it.onTaskEnqueued(task) } // todo valid?
            val res = task.dispatch(context?.get())
            observers.forEach { it.onTaskFinished(task, res) }
            if (executor is ThreadPoolExecutor) {
                Logging.d(
                    TAG,
                    "enqueue [+] <<<<<<<<<<<<<<< [+] task <${task}, ${task.id}> finished information: <tasksCount=${executor.taskCount}, active=${executor.activeCount}, completed=${executor.completedTaskCount}>"
                )
            }
            res
        }))
    }

    fun <K : OnionTask.Result> enqueuePriority(task: OnionTask<K>): OnionFuture<K> { // don't use that too often
        Logging.d(TAG, "enqueuePriority [+] enque high priority task <${task}>")
        return enqueue(task, executorServiceHighPriority)
    }

    fun generateStats(threadPoolExecutor: ThreadPoolExecutor): HashMap<String, Any> {
        val executorStats = HashMap<String, Any>()
        executorStats["activeCount"] = threadPoolExecutor.activeCount
        executorStats["corePoolSize"] = threadPoolExecutor.corePoolSize
        executorStats["largestPoolSize"] = threadPoolExecutor.largestPoolSize
        executorStats["maximumPoolSize"] = threadPoolExecutor.maximumPoolSize
        executorStats["poolSize"] = threadPoolExecutor.poolSize
        executorStats["completedTaskCount"] = threadPoolExecutor.completedTaskCount
        executorStats["taskCount"] = threadPoolExecutor.taskCount
        return executorStats
    }

    fun getStats(): Array<HashMap<String, Any>> {

        val standardExecutorStats = HashMap<String, Any>()
        if (executorService is ThreadPoolExecutor) {
            standardExecutorStats.putAll(generateStats(executorService))
        }

        val highPriorityExecutorStats = HashMap<String, Any>()
        if (executorServiceHighPriority is ThreadPoolExecutor) {
            highPriorityExecutorStats.putAll(generateStats(executorServiceHighPriority))
        }
        return arrayOf(standardExecutorStats, highPriorityExecutorStats)
    }

    fun <K> waitForGlobalTaskResult(resultType: Class<K>): OnionTask.Result? {
        var ret : OnionTask.Result? = null
        val startSignal = CountDownLatch(1)
        addObserver(object : OnionTaskProcessorObserver {
            override fun onTaskEnqueued(task: Any) {

            }

            override fun onTaskFinished(task: Any, result: OnionTask.Result) {
                Logging.d(TAG, "onTaskFinished [+] ${result::class.java} == $resultType")

                if(result::class.java.name.equals(resultType.name)) {
                    Logging.d(TAG, "onTaskFinished [+] found ping result!!")

                    ret = result
                    Logging.d(CheckConnectionTask.TAG, "pingMyselfTest [+] 1 $ret")

                    startSignal.countDown()
                }
            }
        })
        startSignal.await(10000, TimeUnit.MILLISECONDS)
        Logging.d(CheckConnectionTask.TAG, "pingMyselfTest [+] $ret")

        return ret
    }


    interface OnionTaskProcessorObserver {
        fun onTaskEnqueued(task: Any)
        fun onTaskFinished(task: Any, result: OnionTask.Result)
    }

}