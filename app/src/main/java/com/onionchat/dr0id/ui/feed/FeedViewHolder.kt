package com.onionchat.dr0id.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.onionchat.dr0id.R
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.ui.chat.ChatAdapter
import com.onionchat.localstorage.userstore.User

abstract class FeedViewHolder(
    val layoutId: Int, val viewGroup: ViewGroup, var mClickListener: MessageAdapter.ItemClickListener?, val baseView: View = LayoutInflater.from(viewGroup.context)
        .inflate(layoutId, viewGroup, false)
) : RecyclerView.ViewHolder(baseView) {
    val avatarView: ImageView
    val userIdView: TextView
    val dateView: TextView

    abstract fun bind(message: IMessage, user: User?): Boolean

    init {
        avatarView = baseView.findViewById(R.id.feed_fragment_item_header_avatar)
        userIdView = baseView.findViewById(R.id.feed_fragment_item_header_userid)
        dateView = baseView.findViewById(R.id.feed_fragment_item_header_date)


        baseView.setOnClickListener {
            if (mClickListener != null) mClickListener!!.onItemClick(baseView, adapterPosition)
        }
    }
}