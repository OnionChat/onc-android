package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Logging
import com.onionchat.connector.http.OnionClient
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.data.Attachment
import com.onionchat.dr0id.messaging.data.Attachment.Companion.buildAttachmentPath
import com.onionchat.dr0id.queue.OnionTask
import java.security.cert.Certificate

class FetchAttachmentTask(val attachment: Attachment, val cert: Certificate?, val prefetch: Boolean) :
    OnionTask<FetchAttachmentTask.FetchAttachmentResult>() {


    class FetchAttachmentResult(status: Status, exception: Exception? = null) :
        OnionTask.Result(status, exception) {
    }

    override fun run(): FetchAttachmentResult {


        Logging.d(TAG, "run [+] trying to fetch attachment $attachment")

        if (cert == null) {
            Logging.e(TAG, "run [-] no cert specified... we won't be able to verify the attachment $attachment")
        }


        val context = context
        if (context == null) {
            Logging.e(TAG, "run [-] unable to download attachment $attachment context is null")
            return FetchAttachmentResult(Status.FAILURE)
        }
        // 0. check if forwarding is enabled
        var max = 5 * 1024 * 1024 // max in kb
        if (prefetch) {
            context!!.resources?.getInteger(R.integer.max_prefetch_attachment_size_mb)?.let {
                max = it * 1024 * 1024
            }
            if (attachment.size > max) {
                Logging.d(TAG, "run [-] attachment is to big for prefetching <${max}>")
                return FetchAttachmentResult(Status.FAILURE)
            }
        }

        if (attachment.isDownloaded(context)) {
            Logging.d(TAG, "run [+] attachment is already downloaded")
            return FetchAttachmentResult(status = Status.SUCCESS)
        }



        UserManager.getAllOnlineUsers().get().forEach {
            if (it.id != UserManager.myId) { // dont download from myself ;)
                val outputPath = buildAttachmentPath(context, attachment)
                val res = OnionClient.downloadAttachment(it, attachment.attachmentId, outputPath, attachment.size).get()
                if (attachment.isDownloaded(context)) {
                    if (cert != null && !attachment.verify(context, cert)) {
                        Logging.e(TAG, "run [-] error while verify attachment, delete data")
                        outputPath.delete()
                    } else {
                        if (res == OnionClient.DownloadAttachmentResult.DOWNLOADED) {
                            return FetchAttachmentResult(status = Status.SUCCESS)
                        }
                    }
                } else {
                    Logging.e(TAG, "run [-] attachment was not downloaded ${attachment.attachmentId}")
                }
            }
        }

        return FetchAttachmentResult(status = Status.FAILURE)
    }

    override fun onUnhandledException(exception: Exception): FetchAttachmentResult {
        return FetchAttachmentResult(status = Status.FAILURE, exception = exception)
    }

    companion object {
        val TAG = "FetchAttachmentTask"
    }
}