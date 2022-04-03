package com.onionchat.dr0id.ui.feed

import android.content.Context
import android.view.ViewGroup
import com.onionchat.common.DateTimeHelper
import com.onionchat.common.Logging
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.IMessage

class FeedAdapter(dataSet: List<IMessage>, val context: Context) :
    MessageAdapter<FeedViewHolder>(dataSet) {
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): FeedViewHolder {
        // Create a new view, which defines the UI of the list item

        // TODO message types !!
        return when (viewType) {
            ViewHolderType.VIEWTYPE_TEXT_MESSAGE.ordinal -> {
                FeedTextViewHolder(viewGroup, mClickListener)
            }
            ViewHolderType.VIEWTYPE_AUDIO_MESSAGE.ordinal -> {
                FeedAudioViewHolder(viewGroup, mClickListener)
            }
            ViewHolderType.VIEWTYPE_IMAGE_MESSAGE.ordinal -> {
                FeedImageViewHolder(viewGroup, mClickListener)
            }
            ViewHolderType.VIEWTYPE_WEB_MESSAGE.ordinal -> {
                FeedWebViewHolder(viewGroup, mClickListener)
            }
            ViewHolderType.VIEWTYPE_VIDEO_MESSAGE.ordinal -> {
                FeedVideoViewHolder(viewGroup, mClickListener)
            }
//            ChatAdapter.ViewHolderType.VIEWTYPE_ANY_ATTACHMENT_MESSAGE.ordinal -> {
//                Feed(viewGroup, mClickListener)
//            }
            else -> {
                FeedTextViewHolder(viewGroup, mClickListener)
            }
        }
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val message = dataSet[position]//decrypt(encryptedMessage) ?: InvalidMessage("<Decryption Error>")
        val user = UserManager.getUserByHashedId(message.hashedFrom).get()
//        if (user == null) {
//            Logging.e(TAG, "onBindViewHolder [-] unexpected error: user was not found in database... this message should not be displayed!!")
//            return
//        }
        // todo move into super !?
        var userLabel: String = user?.getLastAlias()?.alias ?: message.hashedFrom
        if(message.hashedFrom == UserManager.getMyHashedId()) {
            UserManager.getMyLabel(context)?.let {
                userLabel = it
            }
        }
        holder.userIdView.text = userLabel
        holder.dateView.text = DateTimeHelper.timestampToString(message.created)

        holder.avatarView.setImageBitmap(Conversation.getRepresentativeProfileBitmap(message.hashedFrom))
        Logging.d(TAG, "onBindViewHolder [-] binding viewHolder <${holder}>")
        holder.bind(message, user)

        //         viewHolder.timeText.text = message.getCreationTimeText()
        // todo footer
    }

    companion object {
        const val TAG = "FeedAdapter"
    }
}