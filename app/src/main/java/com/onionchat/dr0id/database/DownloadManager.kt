package com.onionchat.dr0id.database

import android.content.Context
import com.onionchat.common.Logging
import com.onionchat.dr0id.messaging.data.Attachment
import com.onionchat.dr0id.messaging.messages.IAttachmentMessage
import com.onionchat.dr0id.queue.OnionFuture
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.tasks.FetchAttachmentTask
import java.security.cert.Certificate
import java.util.*

object DownloadManager {

    const val TAG = "DownloadManager"

    //val attachmentCache = HashMap<String, ByteArray>() // todo clear cache !! Do we need that shit?
    val downloadTasks = HashMap<String, FetchAttachmentTask>()

    fun enqueueDownload(cert: Certificate?, attachment: Attachment, prefetch: Boolean = false): OnionFuture<FetchAttachmentTask.FetchAttachmentResult>? {
        Logging.d(TAG, "enqueueDownload [+] enqueue download for attachment <${attachment}, $prefetch>")

        val task = if (downloadTasks.containsKey(attachment.attachmentId)) {
            downloadTasks[attachment.attachmentId]!!
        } else {
            FetchAttachmentTask(attachment, cert, prefetch)
        }
        downloadTasks[attachment.attachmentId] = task
        return OnionTaskProcessor.enqueuePriority(task)
    }

    fun getAttachmentBytes(cert: Certificate?, message: IAttachmentMessage, context: Context): ByteArray? {
        val attachment = message.getAttachment()
        if(attachment.cachedAttachmentBytes != null) {
            Logging.d(TAG, "getAttachmentBytes [+] got cached attachment bytes")
            return attachment.cachedAttachmentBytes
        }

        enqueueDownload(cert, attachment)?.get()?.let {
            if (it.status != OnionTask.Status.SUCCESS) {
                Logging.e(TAG, "getAttachmentBytes [-] error while download attachment <${it.status}>")
                return null
            }
        } ?: run {
            Logging.e(TAG, "getAttachmentBytes [-] error while download attachment")
        }
        val loaded = message.getAttachment().loadDecryptedData(cert, context)
        if (loaded == null) {
            Logging.e(TAG, "getAttachmentBytes [-] unable to load decrypted data $attachment")
            return null
        }
        return loaded
    }

    fun getAttachmentBytesAsync(cert: Certificate?, message: IAttachmentMessage, context: Context, callback: (ByteArray?) -> Unit) {
        Thread { // todo move to media player
            callback(getAttachmentBytes(cert, message, context))
        }.start()
    }


}