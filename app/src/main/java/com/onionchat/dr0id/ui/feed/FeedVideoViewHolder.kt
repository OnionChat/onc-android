package com.onionchat.dr0id.ui.feed

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.DownloadManager
import com.onionchat.dr0id.database.MessageManager.getMessagePubByHashedUserId
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.messages.IAttachmentMessage
import com.onionchat.localstorage.userstore.User
import java.io.IOException

class FeedVideoViewHolder(viewGroup: ViewGroup, clickListener: MessageAdapter.ItemClickListener?) :
    FeedViewHolder(R.layout.feed_fragment_item_video, viewGroup, clickListener) {
    var videoView: com.google.android.exoplayer2.ui.StyledPlayerView? = null
    var progressView: ProgressBar? = null
    var imageButton: ImageButton? = null
    var imageView: ImageView? = null

    init {
        imageView = baseView.findViewById(R.id.feed_fragment_item_video_preview)
        imageButton = baseView.findViewById(R.id.feed_fragment_item_video_download)
        videoView = baseView.findViewById(R.id.feed_fragment_item_video_videoview)
        progressView = baseView.findViewById(R.id.feed_fragment_item_video_progress)
    }

    override fun bind(message: IMessage, user: User?): Boolean {
        if (message !is IAttachmentMessage) {
            Logging.e(TAG, "bind [-] invalid message type <${message}>")
            return false
        }
        val videoView = videoView
        if (videoView == null) {
            Logging.e(TAG, "bind [-] ui initialization error [-] videoView is null")
            return false
        }

        val attachment = message.getAttachment()
        // todo check mimetype
        val thumbnail = attachment.thumbnail

        if(thumbnail == null || thumbnail.isEmpty()) {
            Logging.e(TAG, "bind [-] thumbnail is empty...")
        } else {
            imageView?.let {
                Glide.with(viewGroup.context).load(thumbnail).into(it);
            }
        }
        progressView?.visibility = View.GONE
        imageButton?.visibility = View.VISIBLE
        imageView?.visibility = View.VISIBLE


        imageButton?.setOnClickListener {
            progressView?.visibility = View.VISIBLE
            imageButton?.visibility = View.GONE
            imageView?.visibility = View.GONE
            DownloadManager.getAttachmentBytesAsync(getMessagePubByHashedUserId(message.hashedFrom), message, viewGroup.context) {
                if (it == null) {
                    Logging.e(TAG, "bind [-] unable to download attachment")
                    return@getAttachmentBytesAsync
                }
                val byteArrayDataSource = ByteArrayDataSource(it)
                val bytesUri = Uri.parse("byte:///video")
                val dataSpec = DataSpec(bytesUri)
                try {
                    byteArrayDataSource.open(dataSpec)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                val factory = DataSource.Factory { byteArrayDataSource }

                val player = ExoPlayer.Builder( /* context= */viewGroup.context).build()
                Handler(Looper.getMainLooper()).post {
                    progressView?.visibility = View.GONE
                    player.addMediaSource(ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(byteArrayDataSource.uri!!)))
//                    player.playWhenReady = true
                    player.prepare()
                    videoView.setPlayer(player)
                    videoView.player?.playWhenReady = true
                    videoView.player?.play()
                }
            }
        }

        return true
    }

    companion object {
        const val TAG = "FeedImageViewHolder"
    }
}