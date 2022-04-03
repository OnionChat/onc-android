package com.onionchat.dr0id.ui.feed

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.dr0id.media.MediaManager
import com.onionchat.dr0id.media.MediaManagerCallback
import com.onionchat.dr0id.media.MediaManagerState
import com.onionchat.dr0id.messaging.messages.AttachmentMessage
import com.onionchat.dr0id.messaging.messages.IAttachmentMessage
import com.onionchat.dr0id.ui.chat.ChatAdapter
import java.security.cert.Certificate
import java.util.HashMap

// todo use in MessageAdapter !?
class AdapterAudioPlayer(val cert: Certificate?, val message: IAttachmentMessage, var viewHolder: FeedAudioViewHolder, var context: Context) : MediaManagerCallback {



    init {
        prepareUi(MediaManagerState.STOPPED)

        if(MediaManager.attachmentId == message.getAttachment().attachmentId) {
            prepareUi(MediaManager.state)
        }
        MediaManager.callbacks.add(this)
    }

    fun prepareUi(state: MediaManagerState) {
        Logging.d(ChatAdapter.TAG, "prepareUi [+] change state to <$state>")
        when (state) {
            MediaManagerState.PREPARING -> {
                Handler(Looper.getMainLooper()).post {
                    viewHolder?.playButton?.visibility = View.GONE
                    viewHolder?.progressBar?.visibility = View.VISIBLE
                }
            }
            MediaManagerState.PLAYING -> {
                Handler(Looper.getMainLooper()).post {
                    viewHolder?.playButton?.visibility = View.VISIBLE
                    viewHolder?.progressBar?.visibility = View.GONE
                    viewHolder?.playButton?.setImageResource(R.drawable.baseline_stop_circle_white_24)
                }
                viewHolder?.playButton?.setOnClickListener {
                    MediaManager.state = MediaManagerState.STOPPED
                }
            }
            MediaManagerState.STOPPED -> {
                Handler(Looper.getMainLooper()).post {
                    viewHolder?.playButton?.visibility = View.VISIBLE
                    viewHolder?.progressBar?.visibility = View.GONE
                    if (message.getAttachment().isDownloaded(context)) {
                        viewHolder?.playButton?.setImageResource(R.drawable.baseline_play_circle_filled_white_24)
                    } else {
                        viewHolder?.playButton?.setImageResource(R.drawable.baseline_get_app_white_24)
                    }
                }
                viewHolder?.playButton?.setOnClickListener {
                    playAudioMessage()
                }
            }
            MediaManagerState.ERROR -> {

            }
        }
    }

    fun playAudioMessage() {
        MediaManager.playAudioMessage(cert, message, context)
    }

    override fun onStateChanged(attachmentId: String?, state: MediaManagerState) {
        Logging.d(ChatAdapter.TAG, "onStateChanged [+] <$attachmentId, $state>")
        if(attachmentId == message.getAttachment().attachmentId) {
            prepareUi(state)
        }
    }
}

