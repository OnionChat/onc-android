package com.onionchat.dr0id.ui.chat

import android.graphics.Color
import android.text.util.Linkify
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.onionchat.connector.Communicator
import com.onionchat.dr0id.R
import com.onionchat.dr0id.messaging.messages.Message
import com.onionchat.dr0id.messaging.messages.TextMessage
import com.onionchat.dr0id.users.UserManager
import com.onionchat.common.extensions.handleUrlClicks

class ChatAdapter(private val dataSet: List<Message>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    var mClickListener: ItemClickListener? = null

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, var mClickListener: ItemClickListener?) : RecyclerView.ViewHolder(view) {
        val textView: TextView

        init {
            // Define click listener for the ViewHolder's View.
            textView = view.findViewById(R.id.item_text)
            view.setOnClickListener {
                if (mClickListener != null) mClickListener!!.onItemClick(view, adapterPosition)
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.activity_chat_window_list_text_item, viewGroup, false)
        // TODO message types !!
        return ViewHolder(view, mClickListener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        if (dataSet[position] is TextMessage) {
            viewHolder.textView.text = (dataSet[position] as TextMessage).getText()
            Linkify.addLinks(viewHolder.textView, Linkify.WEB_URLS);
            viewHolder.textView.handleUrlClicks {
                mClickListener?.onUrlClicked(it)
            }
            if (UserManager.myId != dataSet[position].from) {
                viewHolder.textView.gravity = Gravity.LEFT
            } else {
                viewHolder.textView.gravity = Gravity.RIGHT
            }


            if(dataSet[position].status == Communicator.MessageSentStatus.ERROR) {
                viewHolder.textView.setTextColor(Color.RED) // error
            } else if(dataSet[position].status == Communicator.MessageSentStatus.SENT) {
                if(dataSet[position].read) {
                    viewHolder.textView.setTextColor(Color.GREEN) // read
                } else {
                    viewHolder.textView.setTextColor(Color.BLUE) // sent
                }
            } else {
                viewHolder.textView.setTextColor(Color.LTGRAY) // ongoing
            }

        } else {
            viewHolder.textView.text = "UNSUPPORTED MESSAGE (" + dataSet[position].javaClass.simpleName + ")"
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener) {
        this.mClickListener = itemClickListener
    }

    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
        fun onUrlClicked(url:String)
    }
}
