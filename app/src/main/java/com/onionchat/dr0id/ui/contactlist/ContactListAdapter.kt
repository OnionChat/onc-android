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
import com.onionchat.dr0id.R
import com.onionchat.localstorage.userstore.Conversation

class ContactListAdapter(private val dataSet: List<Conversation>, private val selection: Boolean = false) :
    RecyclerView.Adapter<ContactListAdapter.ViewHolder>() {
    var mClickListener: ItemClickListener? = null

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, var mClickListener: ItemClickListener?) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        val messageCounter: TextView
        val avatar: ImageView
        val checkBox: CheckBox

        init {
            // Define click listener for the ViewHolder's View.
            textView = view.findViewById(R.id.contact_id)
            messageCounter = view.findViewById(R.id.unread_messages_count)
            avatar = view.findViewById(R.id.contact_avatar)
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

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        if (selection) {
            viewHolder.checkBox.visibility = View.VISIBLE
            if(dataSet[position].selected) {
                viewHolder.checkBox.isChecked = true
            }
            viewHolder.checkBox.setOnCheckedChangeListener {i, v ->
                dataSet[position].selected = viewHolder.checkBox.isChecked
                mClickListener?.onCheckedChangeListener(position)
            }
        }
        viewHolder.textView.text = dataSet[position].getLabel()
        viewHolder.messageCounter.text = "" + dataSet[position].unreadMessages
        if (dataSet[position].unreadMessages > 0) {
            viewHolder.messageCounter.visibility = View.VISIBLE
            viewHolder.messageCounter.setTextColor(Color.GREEN)
            viewHolder.messageCounter.setTypeface(null, BOLD)
            viewHolder.textView.setTypeface(null, BOLD)
        } else {
            viewHolder.messageCounter.visibility = View.GONE
            viewHolder.textView.setTypeface(null, NORMAL)
        }
        if (dataSet[position].isOnline) {
            viewHolder.textView.setTextColor(Color.GREEN)
        } else {
            viewHolder.textView.setTextColor(Color.WHITE)
        }
        viewHolder.avatar.setImageBitmap(dataSet[position].getAvatar())

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener) {
        this.mClickListener = itemClickListener
    }

    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
        fun onItemLongClick(view: View?, position: Int)
        fun onCheckedChangeListener(position: Int)
    }
}
