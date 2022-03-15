package com.onionchat.dr0id.ui.chat

import android.graphics.Color
import android.text.util.Linkify
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.common.MessageStatus
import com.onionchat.common.extensions.handleUrlClicks
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.AsymmetricMessage
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.dr0id.messaging.messages.ITextMessage
import com.onionchat.dr0id.messaging.messages.InvalidMessage
import java.text.SimpleDateFormat
import java.util.*


class ChatAdapter(private val dataSet: List<IMessage>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    var mClickListener: ItemClickListener? = null

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, var mClickListener: ItemClickListener?) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        val dateText: TextView
        val timeText: TextView
        val bubble: RelativeLayout
        val bubbtleSpace: LinearLayout

        init {
            // Define click listener for the ViewHolder's View.
            textView = view.findViewById(R.id.item_text)
            dateText = view.findViewById(R.id.item_date)
            timeText = view.findViewById(R.id.item_time)
            bubble = view.findViewById(R.id.item_bubble)
            bubbtleSpace = view.findViewById(R.id.bubble_space)
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

    fun getCalendar(stamp: Long): Calendar {
        val c: Calendar = Calendar.getInstance()
        c.setTimeInMillis(stamp)
        return c
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element

        // date
        viewHolder.bubble.visibility = View.VISIBLE
        viewHolder.textView.text = ""
        val message = dataSet[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        viewHolder.dateText.text = dateFormat.format(Date(message.created))
        val current = getCalendar(message.created)

        if (position - 1 >= 0) {
            val msgBefore = dataSet[position - 1]
            getCalendar(msgBefore.created)?.let { last ->
                if (last.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
                    last.get(Calendar.MONTH) == current.get(Calendar.MONTH) &&
                    last.get(Calendar.DAY_OF_MONTH) == current.get(Calendar.DAY_OF_MONTH)

                ) {
                    viewHolder.dateText.visibility = View.GONE
                } else {
                    viewHolder.dateText.visibility = View.VISIBLE
                }

            }
        }

        // time
        val timeFormat = SimpleDateFormat("HH:mm:ss")

        viewHolder.timeText.text = timeFormat.format(Date(message.created))

        // color
        if (message is SymmetricMessage) {
            if (message is ITextMessage) {
                viewHolder.textView.text = message.getText().text // todo formatting
            }
            Linkify.addLinks(viewHolder.textView, Linkify.WEB_URLS);
            viewHolder.textView.handleUrlClicks {
                mClickListener?.onUrlClicked(it)
            }
            if (IDGenerator.toHashedId(UserManager.myId!!) != message.hashedFrom) {
                viewHolder.bubbtleSpace.gravity = Gravity.LEFT
                viewHolder.timeText.gravity = Gravity.LEFT

            } else {
                viewHolder.bubbtleSpace.gravity = Gravity.RIGHT
                viewHolder.timeText.gravity = Gravity.RIGHT
            }


//            if(MessageStatus.hasFlag(dataSet[position].messageStatus, MessageStatus.NOT_SENT)) {
//                viewHolder.textView.setTextColor(Color.RED) // error
//            } else

            Logging.d(TAG, "onBindViewHolder [+] status ${MessageStatus.dumpState(message.messageStatus)}")
            if (MessageStatus.hasFlag(message.messageStatus, MessageStatus.READ)) {
                viewHolder.timeText.setTextColor(Color.GREEN) // read
            } else if (MessageStatus.hasFlag(message.messageStatus, MessageStatus.SENT)) {
                viewHolder.timeText.setTextColor(Color.BLUE) // sent
            } else {
                viewHolder.timeText.setTextColor(Color.WHITE) // ongoing
            }
        } else if (message is AsymmetricMessage) {
            viewHolder.dateText.text = "KEY NEGOTIATION <${message.messageId}>" // todo formatting
            viewHolder.dateText.visibility = View.VISIBLE
            viewHolder.bubble.visibility = View.GONE
        } else if (message is InvalidMessage) {
            viewHolder.textView.text = message.getText().text // todo formatting
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
        fun onUrlClicked(url: String)
    }

    companion object {
        const val TAG = "ChatAdapter"
    }
}
