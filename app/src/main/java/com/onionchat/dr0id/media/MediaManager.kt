package com.onionchat.dr0id.media

import android.content.Context
import android.media.AudioManager
import android.media.AudioTrack
import android.media.AudioTrack.PLAYSTATE_PLAYING
import com.onionchat.common.Logging
import com.onionchat.dr0id.database.DownloadManager.getAttachmentBytesAsync
import com.onionchat.dr0id.media.stream.StreamController
import com.onionchat.dr0id.messaging.messages.AttachmentMessage
import com.onionchat.dr0id.messaging.messages.IAttachmentMessage
import java.security.cert.Certificate

enum class MediaManagerState {
    PLAYING,
    STOPPED,
    PREPARING,
    ERROR
}

interface MediaManagerCallback {
    fun onStateChanged(attachmentId: String?, state: MediaManagerState)
}

object MediaManager {
    val callbacks = HashSet<MediaManagerCallback>()
    val controller = StreamController()
    var attachmentId: String? = null
        set(it) {
            callbacks.forEach {
                it.onStateChanged(attachmentId, MediaManagerState.STOPPED)
            }
            field = it
        }

    const val TAG = "MediaManager"

    var state = MediaManagerState.STOPPED
        set(value) {
            Logging.d(TAG, "state [+] changed to <$value>")

            if (value == MediaManagerState.STOPPED) {
                controller.stop()
            }
            callbacks.forEach { callback ->
                callback.onStateChanged(attachmentId, value)
            }
            field = value
        }

    fun playAudioMessage(cert: Certificate?, message: IAttachmentMessage, context: Context) {
        state = MediaManagerState.STOPPED
        attachmentId = message.getAttachment().attachmentId
        state = MediaManagerState.PREPARING
        getAttachmentBytesAsync(cert, message, context) {
            if (it == null) {
                state = MediaManagerState.ERROR
                return@getAttachmentBytesAsync
            }
            state = MediaManagerState.PLAYING


            val track = controller.playAudioStream(AudioManager.STREAM_MUSIC)
            if (track == null) {
                Logging.e(TAG, "playAudioMessage [-] unable to create AudioTrack object")
            } else {
                track.notificationMarkerPosition = (it.size); // pcm16 would be ==  / 2
                track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(p0: AudioTrack?) {
                        state = MediaManagerState.STOPPED
                    }

                    override fun onPeriodicNotification(p0: AudioTrack?) { // todo update current state !
                    }

                })
                it?.let {
                    track.write(it, 0, it.size)
                }
            }
        }
    }
}