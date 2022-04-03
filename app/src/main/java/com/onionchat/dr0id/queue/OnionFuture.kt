package com.onionchat.dr0id.queue

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class OnionFuture<K : OnionTask.Result>(val executor: ExecutorService, private val future: Future<K>) {


    fun get(): K {
        return future.get()
    }

    fun then(callback: (K) -> Unit) {
        executor.submit {
            callback(future.get())
        }
    }
}