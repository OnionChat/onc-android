package com.onionchat.dr0id.ui.viewer

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.common.MimeTypes
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.MessageManager.getMessagePub
import com.onionchat.dr0id.messaging.MessageProcessor
import com.onionchat.dr0id.messaging.messages.AttachmentMessage
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.tasks.FetchAttachmentTask
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class ImageViewer : AppCompatActivity() {
    private val viewModel: MediaViewerModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_viewer_activity)
        setupViewModel()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ImageViewerFragment.newInstance())
                .commitNow()
        }
    }

    fun setupViewModel() {
        if (intent.hasExtra(EXTRA_ATTACHMENT_MESSAGE_ID)) {
            runBlocking {
                openImageViaAttachmentMessageAsync()
            }

        } else {
            setError("Invalid usage", 1)
        }
    }

    suspend fun openImageViaAttachmentMessageAsync() = GlobalScope.async {
        async {
            val messageId = intent.getStringExtra(EXTRA_ATTACHMENT_MESSAGE_ID)
            if (messageId == null) {
                setError("Invalid payload", 2)
                return@async
            }
            Logging.d(TAG, "openImageViaAttachmentMessageAsync [+] going to open image $messageId")
            val encryptedMessage = MessageManager.getMessageById(messageId).get() // todo async?
            if (encryptedMessage == null) {
                setError("Message not found", 3)
                return@async
            }
            val cert = getMessagePub(encryptedMessage)
            if(cert == null) {
                setError("Unable to get crypto information", 8)
                return@async
            }

            try {
                val decryptedMessage = MessageProcessor.unpack(encryptedMessage, cert, Crypto.getMyKey())
                if (decryptedMessage == null) {
                    setError("Unable to get decrypted content", 4)
                    return@async
                }
                if (decryptedMessage is AttachmentMessage) {
                    val attachment = decryptedMessage.getAttachment()
                    if (MimeTypes.isSupportedImage(attachment.mimetype)) {
                        if (attachment.isDownloaded(this@ImageViewer)) {
                            applyImage(attachment.loadDecryptedData(cert, this@ImageViewer), attachment.mimetype)
                        } else {
                            OnionTaskProcessor.enqueuePriority(FetchAttachmentTask(attachment, cert, false)).then {
                                if (attachment.isDownloaded(this@ImageViewer)) {
                                    applyImage(attachment.loadDecryptedData(cert, this@ImageViewer), attachment.mimetype)
                                } else {
                                    setError("Unable to download attachment", 10)
                                }
                            }
                        }
                    } else {
                        setError("Unsupported mimetype <${attachment.mimetype}>", 7)
                        return@async
                    }
                } else {
                    setError("Invalid message ${decryptedMessage::class.java.simpleName}", 6)
                    return@async
                }
            } catch (exception: Exception) {
                Logging.e(TAG, "Unable to decrypt message ${encryptedMessage.messageId}", exception)
                setError("Unable to decrypt content <${exception.message}>", 5)
                return@async
            }
        }
    }

    fun applyImage(image: ByteArray?, mimetype: String) {
        runOnUiThread {
            viewModel.image.value = Glide.with(this@ImageViewer).load(image)
        }
    }

    fun setError(message: String, code: Int) {
        Logging.e(TAG, "setError <${message}, $code>")
        runOnUiThread {
            viewModel.error.value = "$message $code"
        }
    }

    companion object {
        const val EXTRA_ATTACHMENT_MESSAGE_ID = "attachment_message_id"

        const val TAG = "ImageViewer"
    }
}