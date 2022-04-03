package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.connector.http.OnionClient
import com.onionchat.dr0id.connectivity.PingPayload
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.userstore.PingInfo
import java.util.*

class PingTask(val onionUrl: String? = null, val payload: PingPayload = PingPayload()) : OnionTask<PingTask.PingResult>() {


    class PingResult(status: Status, val pingInfo: PingInfo? = null, exception: Exception? = null) : OnionTask.Result(status, exception) {
    }

    override fun run(): PingResult {

        if (onionUrl == null) {
            Logging.d(TAG, "run [+] onionUrl is null... let's ping ourself")
        }
        val onionUrl = onionUrl ?: UserManager.myId
        if (onionUrl == null) {
            Logging.d(TAG, "run [+] cannot run ping... onion url is null")
            return PingResult(Status.FAILURE)
        }
        val context = context
        if (context == null) {
            Logging.e(TAG, "run [-] context is null!! Abort.")
            return PingResult(Status.FAILURE)
        }
        val myId = UserManager.myId
        if (myId == null) {
            Logging.e(TAG, "run [-] myId is null. Abort.")
            return PingResult(Status.FAILURE)
        }
        val encodedPayload = PingPayload.encode(payload)
        if (encodedPayload == null) {
            Logging.e(TAG, "run [-] encodedPayload is null. Abort.")
            return PingResult(Status.FAILURE)
        }
        var taskStatus = Status.FAILURE
        val result = OnionClient.ping(myId, onionUrl).get()
        var status = PingInfo.PingInfoStatus.FAILURE.ordinal
        if (result) {
            taskStatus = Status.SUCCESS
            status = PingInfo.PingInfoStatus.SUCCESS.ordinal
        }
        val info = PingInfo(UUID.randomUUID().toString(), IDGenerator.toHashedId(onionUrl), payload.purpose, status, 1, System.currentTimeMillis(), "")
        UserManager.storePingInfo(info)
        return PingResult(taskStatus, info)
    }

    override fun onUnhandledException(exception: java.lang.Exception): PingResult {
        return PingResult(status = Status.FAILURE, exception = exception)
    }

    companion object {
        const val TAG = "PingTask"
    }
}