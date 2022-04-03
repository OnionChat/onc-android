package com.onionchat.dr0id.ui.feed

import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.MessageManager.getMessagePub
import com.onionchat.dr0id.database.MessageManager.getMessagePubByHashedUserId
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.messages.IAttachmentMessage
import com.onionchat.dr0id.ui.chat.ChatAdapter
import com.onionchat.localstorage.userstore.User
import java.util.HashMap

class FeedAudioViewHolder(viewGroup: ViewGroup, clickListener: MessageAdapter.ItemClickListener?) : // todo refactor ChatAdapter.ItemClickListener
    FeedViewHolder(R.layout.feed_fragment_item_audio, viewGroup, clickListener) {
        var progressBar :ProgressBar? = null
        var playButton :ImageButton? = null

        init {
            progressBar = baseView.findViewById(R.id.feed_fragment_item_audio_progress)
            playButton = baseView.findViewById(R.id.feed_fragment_item_audio_play_button)
        }

    override fun bind(message: IMessage, user: User?) : Boolean {
        if (message !is IAttachmentMessage) {
            Logging.e(FeedImageViewHolder.TAG, "bind [-] invalid message type <${message}>")
            return false
        }
        val progressView = progressBar
        if (progressView == null) {
            Logging.e(FeedImageViewHolder.TAG, "bind [-] ui initialization error [-] progressView is null")
            return false
        }
        val playButton = playButton
        if (playButton == null) {
            Logging.e(FeedImageViewHolder.TAG, "bind [-] ui initialization error [-] playButton is null")
            return false
        }
        val attachment = message.getAttachment()

        val attachmentId = attachment.attachmentId
        if (!players.containsKey(attachmentId)) {
            players[attachmentId] = AdapterAudioPlayer(getMessagePubByHashedUserId(message.hashedFrom), message, this, viewGroup.context)
        }
        players[attachmentId]?.viewHolder = this

        return true
    }

    companion object {
        val players = HashMap<String, AdapterAudioPlayer>() // todo fix memory leak
    }
}