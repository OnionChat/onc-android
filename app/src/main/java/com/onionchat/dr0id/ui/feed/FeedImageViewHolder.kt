package com.onionchat.dr0id.ui.feed

import android.view.ViewGroup
import android.widget.ImageButton
import com.bumptech.glide.Glide
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.messages.IAttachmentMessage
import com.onionchat.dr0id.ui.ActivityLauncher
import com.onionchat.localstorage.userstore.User

class FeedImageViewHolder(viewGroup: ViewGroup, clickListener: MessageAdapter.ItemClickListener?) :
    FeedViewHolder(R.layout.feed_fragment_item_image, viewGroup, clickListener) {
    var imageButton: ImageButton? = null

    init {
        imageButton = baseView.findViewById(R.id.feed_fragment_item_web_image_imagebutton)
    }

    override fun bind(message: IMessage, user: User?): Boolean {
        if (message !is IAttachmentMessage) {
            Logging.e(TAG, "bind [-] invalid message type <${message}>")
            return false
        }
        val imageButton = imageButton
        if (imageButton == null) {
            Logging.e(TAG, "bind [-] ui initialization error [-] imageView is null")
            return false
        }
        val attachment = message.getAttachment()
        // todo check mimetype
        val thumbnail = attachment.thumbnail
        if(thumbnail == null) {
            Logging.e(TAG, "bind [-] attachment doesn't have thumbnail... abort")
            return false
        }
        if(thumbnail.isEmpty()) {
            Logging.e(TAG, "bind [-] thumbnail is empty... abort")
            return false
        }
        Glide.with(viewGroup.context).load(thumbnail).into(imageButton);
        imageButton.setOnClickListener {
            // todo open image!?
            ActivityLauncher.openImageViewerForMessage(message, viewGroup.context)
        }
        return true
    }

    companion object {
        const val TAG = "FeedImageViewHolder"
    }
}