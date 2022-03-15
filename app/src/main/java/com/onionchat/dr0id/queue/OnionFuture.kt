package com.onionchat.dr0id.queue

import java.util.concurrent.Future

class OnionFuture<K : OnionTask.Result>(private val future: Future<K>) {


    fun get(): K {
        return future.get()
    }

    fun then(callback: (K) -> Unit) {
        Thread {
            callback(future.get())
        }.start()
    }
}