package com.onionchat.dr0id.ui.contactlist

import android.graphics.Color
import android.graphics.Typeface.BOLD
import android.graphics.Typeface.NORMAL
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.MessageManager.getMessagePub
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.MessageProcessor
import com.onionchat.dr0id.messaging.messages.*
import com.onionchat.dr0id.ui.chat.ChatAdapter
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.ConversationType
import com.onionchat.localstorage.userstore.User

class ContactListAdapter(private val dataSet: List<Conversation>, private val selection: Boolean = false) :
    RecyclerView.Adapter<ContactListAdapter.ViewHolder>() {
    var mClickListener: ItemClickListener? = null

    val myKey = Crypto.getMyKey()

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, var mClickListener: ItemClickListener?) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        val messageCounter: TextView
        val lastMessage: TextView
        val avatar: ImageView
        val conversationTypeView: ImageView
        val checkBox: CheckBox

        init {
            // Define click listener for the ViewHolder's View.
            textView = view.findViewById(R.id.contact_label)
            messageCounter = view.findViewById(R.id.unread_messages_count)
            avatar = view.findViewById(R.id.contact_avatar)
            conversationTypeView = view.findViewById(R.id.conversation_type)
            lastMessage = view.findViewById(R.id.last_message)
            checkBox = view.findViewById(R.id.contact_checked)
            view.setOnClickListener {
                if (mClickListener != null) mClickListener!!.onItemClick(view, adapterPosition)
            }
            view.setOnLongClickListener {
                if (mClickListener != null) mClickListener!!.onItemLongClick(view, adapterPosition)
                true
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.activity_contact_list_row, viewGroup, false)

        return ViewHolder(view, mClickListener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val conversation = dataSet[position]
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        if (selection) {
            viewHolder.checkBox.visibility = View.VISIBLE
            if (conversation.selected) {
                viewHolder.checkBox.isChecked = true
            }
            viewHolder.checkBox.setOnCheckedChangeListener { i, v ->
                conversation.selected = viewHolder.checkBox.isChecked
                mClickListener?.onCheckedChangeListener(position)
            }
        }
        viewHolder.textView.text = conversation.getLabel()
        viewHolder.lastMessage.text = ""
        conversation.lastMessage?.let { message ->
            conversation.user?.let { user ->
                val decryptedMessage = decrypt(user, message)
                if (decryptedMessage is ITextMessage) {
                    viewHolder.lastMessage.text = decryptedMessage.getText().text
                } else if (decryptedMessage is IAttachmentMessage) {
                    viewHolder.lastMessage.text = "Attachment" // todo get from strings
                }
            }
        }
        viewHolder.messageCounter.text = "" + dataSet[position].unreadMessages
        if (conversation.unreadMessages > 0) {
            viewHolder.messageCounter.visibility = View.VISIBLE
            viewHolder.messageCounter.setTextColor(Color.GREEN)
            viewHolder.messageCounter.setTypeface(null, BOLD)
            viewHolder.textView.setTypeface(null, BOLD)
        } else {
            viewHolder.messageCounter.visibility = View.GONE
            viewHolder.textView.setTypeface(null, NORMAL)
        }
        if (conversation.isOnline) {
            viewHolder.textView.setTextColor(Color.GREEN)
        } else {
            viewHolder.textView.setTextColor(Color.WHITE)
        }
        viewHolder.avatar.setImageBitmap(dataSet[position].getAvatar())

        // conversation type
        if(conversation.getConversationType() == ConversationType.BROADCAST) {
            viewHolder.conversationTypeView.visibility = View.VISIBLE
        } else {
            viewHolder.conversationTypeView.visibility = View.GONE
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener) {
        this.mClickListener = itemClickListener
    }

    fun decrypt(partner: User, encryptedMessage: EncryptedMessage): IMessage? {
        try {
            return MessageProcessor.unpack(encryptedMessage, getMessagePub(partner, encryptedMessage), myKey)
        } catch (exception: Exception) {
            Logging.e(ChatAdapter.TAG, "Unable to decrypt message ${encryptedMessage.messageId}", exception)
            return InvalidMessage("<Decryption Error (${exception.message}>")
        }
    }


    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
        fun onItemLongClick(view: View?, position: Int)
        fun onCheckedChangeListener(position: Int)
    }
}
