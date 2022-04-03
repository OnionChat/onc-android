package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.connector.http.OnionClient
import com.onionchat.dr0id.connectivity.PingPayload
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.localstorage.userstore.PingInfo
import java.util.*

class HandlePingTask(val data:String) : OnionTask<HandlePingTask.HandlePingResult>() {


    class HandlePingResult(status: Status, val payload: PingPayload?, exception: Exception? = null) : OnionTask.Result(status, exception) {
    }

    override fun run(): HandlePingResult {
        val pingData = PingPayload.decode(data)
        pingData?.let {
            OnionTaskProcessor.enqueue(ProcessPendingTask(UserManager.getUserByHashedId(it.uid).get()))
            val info = PingInfo(UUID.randomUUID().toString(), it.uid, it.purpose, PingInfo.PingInfoStatus.RECEIVED.ordinal, 1, System.currentTimeMillis(), "")
            UserManager.storePingInfo(info)
        }
        return HandlePingResult(Status.SUCCESS, pingData)
    }

    override fun onUnhandledException(exception: java.lang.Exception): HandlePingResult {
        return HandlePingResult(status = Status.FAILURE, payload = null, exception = exception)
    }

    companion object {
        const val TAG = "HandlePingTask"
    }
}