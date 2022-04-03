package com.onionchat.dr0id.ui.feed

import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.common.MimeTypes
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.messages.AttachmentMessage
import com.onionchat.dr0id.ui.chat.ChatAdapter


// todo move to other package
abstract class MessageAdapter <K : RecyclerView.ViewHolder>(val dataSet: List<IMessage>) :
    RecyclerView.Adapter<K>()  {

    var mClickListener: ItemClickListener? = null


    enum class ViewHolderType {
        VIEWTYPE_TEXT_MESSAGE,
        VIEWTYPE_AUDIO_MESSAGE,
        VIEWTYPE_IMAGE_MESSAGE,
        VIEWTYPE_ANY_ATTACHMENT_MESSAGE,
        VIEWTYPE_WEB_MESSAGE,
        VIEWTYPE_VIDEO_MESSAGE
    }

    override fun getItemViewType(position: Int): Int {
        // Just as an example, return 0 or 2 depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous
        val decrypted = dataSet[position]
        when (decrypted.type) {
            MessageTypes.TEXT_MESSAGE.ordinal -> {
                return ChatAdapter.ViewHolderType.VIEWTYPE_TEXT_MESSAGE.ordinal
            }
            MessageTypes.ATTACHMENT_MESSAGE.ordinal -> {
                if (decrypted is AttachmentMessage) {
                    val attachment = decrypted.getAttachment()
                    Logging.d(TAG, "getItemViewType [+] mimetype ${attachment.mimetype}")
                    if (MimeTypes.isSupportedAudio(attachment.mimetype)) {
                        return ViewHolderType.VIEWTYPE_AUDIO_MESSAGE.ordinal
                    } else if (MimeTypes.isSupportedImage(attachment.mimetype)) {
                        return ViewHolderType.VIEWTYPE_IMAGE_MESSAGE.ordinal
                    } else if(MimeTypes.isSupportedVideo(attachment.mimetype)) {
                        return ViewHolderType.VIEWTYPE_VIDEO_MESSAGE.ordinal
                    } else {
                        // fallback
                    }
                }
                // todo get from decrypted message!?
                return ViewHolderType.VIEWTYPE_AUDIO_MESSAGE.ordinal
            }
            else -> {
                // fallback
                return ViewHolderType.VIEWTYPE_TEXT_MESSAGE.ordinal
            }
        }
    }

    @CallSuper
    override fun onBindViewHolder(holder: K, position: Int) {
        checkMoreMessagesRequired(position)

    }

    override fun getItemCount() = dataSet.size

    fun checkMoreMessagesRequired(position: Int) {
        Logging.d(ChatAdapter.TAG, "checkMoreMessagesRequired [+] <$position, ${dataSet.size}>")
        if ((dataSet.size - position) >= dataSet.size / 2) { // todo half of dataset?
            onMoreMessagesRequiredListener?.let {
                it(position)
            }
        }
    }

    fun setClickListener(itemClickListener: ItemClickListener) {
        this.mClickListener = itemClickListener
    }

    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
        fun onUrlClicked(url: String)
    }

    var onMoreMessagesRequiredListener: ((Int) -> Unit)? = null

    companion object {
        const val TAG = "MessageAdapter"
    }
}