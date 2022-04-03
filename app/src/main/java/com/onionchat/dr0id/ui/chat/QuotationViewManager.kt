package com.onionchat.dr0id.ui.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.R
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.messages.AttachmentMessage
import com.onionchat.dr0id.messaging.messages.TextMessage
import kotlinx.atomicfu.atomic

class QuotationViewManager(val context: Context) {

    val cache = atomic(HashMap<String, View>())

    fun getQuotationViewFromCache(quotedMessageId: String): View? {
        return cache.value.get(quotedMessageId)
    }

    fun createQuotationView(quotedMessage: IMessage, rootView: ViewGroup): View? {
        getQuotationViewFromCache(quotedMessage.messageId)?.let {
            return@createQuotationView it
        }
        when (quotedMessage.type) {
            MessageTypes.TEXT_MESSAGE.ordinal -> {
                if (quotedMessage is TextMessage) {
                    return createTextMessageQuotationView(quotedMessage, rootView)
                } else {
                    Logging.e(TAG, "createQuotationView [-] invalid message type $quotedMessage for text message")
                    return null
                }
            }

            MessageTypes.ATTACHMENT_MESSAGE.ordinal -> {
                if (quotedMessage is AttachmentMessage) {
                    return createAttachmentMessageQuotationView(quotedMessage, rootView)
                } else {
                    Logging.e(TAG, "createQuotationView [-] invalid message type $quotedMessage for attachment message")
                    return null
                }
            }
            else -> {
                Logging.e(TAG, "createQuotationView [-] unsupported message type $quotedMessage")
                return null
            }
        }
    }

    fun createTextMessageQuotationView(textMessage: TextMessage, rootView: ViewGroup): View? {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.quotation_view_text_message, rootView, false) // todo improve root!?
        val textView = view.findViewById<TextView>(R.id.quotation_view_text_item_text)
        val timeView = view.findViewById<TextView>(R.id.quotation_view_text_item_time)
        textView.text = textMessage.getText().text
        timeView.text = textMessage.getCreationTimeText()
        return view
    }

    fun createAttachmentMessageQuotationView(attachmentMessage: AttachmentMessage, rootView: ViewGroup): View? {

        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.quotation_view_attachment_message, rootView, false) // todo improve root!?
        val textView = view.findViewById<TextView>(R.id.quotation_view_attachment_item_text)
        val timeView = view.findViewById<TextView>(R.id.quotation_view_attachment_item_time)
        textView.text = attachmentMessage.getAttachment().mimetype
        timeView.text = attachmentMessage.getCreationTimeText()
        return view
    }

    companion object {
        const val TAG = "QuotationViewBuilder"
    }
}