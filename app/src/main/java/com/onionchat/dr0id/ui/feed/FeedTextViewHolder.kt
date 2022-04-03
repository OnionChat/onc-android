package com.onionchat.dr0id.ui.feed

import android.text.util.Linkify
import android.view.ViewGroup
import android.widget.TextView
import com.onionchat.common.Logging
import com.onionchat.common.extensions.handleUrlClicks
import com.onionchat.dr0id.R
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.messages.ITextMessage
import com.onionchat.dr0id.ui.chat.ChatAdapter
import com.onionchat.localstorage.userstore.User

class FeedTextViewHolder(viewGroup: ViewGroup, clickListener: MessageAdapter.ItemClickListener?) :
    FeedViewHolder(R.layout.feed_fragment_item_text, viewGroup, clickListener) {
    var textView: TextView? = null

    init {
        textView = baseView.findViewById(R.id.feed_fragment_item_text_text)
    }

    override fun bind(message: IMessage, user: User?): Boolean {
        if (message !is ITextMessage) {
            Logging.e(TAG, "bind [-] invalid message type <${message}>")
            return false
        }
        val textView = textView
        if(textView == null) {
            Logging.e(TAG, "bind [-] ui initialization error [-] textView is null")
            return false
        }
        textView.text = message.getText().text
        Linkify.addLinks(textView, Linkify.WEB_URLS);
        textView.handleUrlClicks {
            mClickListener?.onUrlClicked(it)
        }
        return true
    }

    companion object {
        const val TAG = "FeedTextViewHolder"
    }
}