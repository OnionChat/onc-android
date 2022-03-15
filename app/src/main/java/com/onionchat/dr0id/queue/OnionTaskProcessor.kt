package com.onionchat.dr0id.queue

import java.util.concurrent.Executors

object OnionTaskProcessor {

    val executorService = Executors.newSingleThreadExecutor()

    fun enqueue(task:OnionTask) {

    }
}