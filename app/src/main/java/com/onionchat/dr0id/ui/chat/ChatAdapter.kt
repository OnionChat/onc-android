package com.onionchat.dr0id.ui.chat

import android.app.Activity
import android.content.Context
import android.graphics.Color

import android.os.Handler
import android.os.Looper
import android.text.util.Linkify
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.onionchat.common.*
import com.onionchat.common.MimeTypes.isSupportedAudio
import com.onionchat.common.MimeTypes.isSupportedImage
import com.onionchat.common.extensions.getColorFromAttr
import com.onionchat.common.extensions.handleUrlClicks
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.DownloadManager
import com.onionchat.dr0id.database.DownloadManager.getAttachmentBytesAsync
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.media.MediaManager
import com.onionchat.dr0id.media.MediaManagerCallback
import com.onionchat.dr0id.media.MediaManagerState
import com.onionchat.dr0id.media.stream.StreamController
import com.onionchat.dr0id.messaging.AsymmetricMessage
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.IMessage.Companion.EXTRA_QUOTED_MESSAGE_ID
import com.onionchat.dr0id.messaging.MessageProcessor
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.dr0id.messaging.messages.AttachmentMessage
import com.onionchat.dr0id.messaging.messages.ITextMessage
import com.onionchat.dr0id.messaging.messages.InvalidMessage
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.tasks.FetchAttachmentTask
import com.onionchat.dr0id.ui.ActivityLauncher.openImageViewerForMessage
import com.onionchat.localstorage.messagestore.EncryptedMessage
import org.json.JSONObject
import java.security.Key
import java.security.cert.Certificate
import java.util.*


class ChatAdapter(
    private val dataSet: List<IMessage>,
    val partnerPub: Certificate?,
    val myPub: Certificate?,
    val myKey: Key = Crypto.getMyKey(),
    val context: Context,
    val showUserNames: Boolean = false
) :
    RecyclerView.Adapter<ChatAdapter.BaseViewHolder>() {

    enum class ViewHolderType {
        VIEWTYPE_TEXT_MESSAGE,
        VIEWTYPE_AUDIO_MESSAGE,
        VIEWTYPE_IMAGE_MESSAGE,
        VIEWTYPE_ANY_ATTACHMENT_MESSAGE
    }

    var quotationViewManager = QuotationViewManager(context)

    var mClickListener: ItemClickListener? = null

    open class BaseViewHolder(
        val layoutId: Int, val viewGroup: ViewGroup, var mClickListener: ItemClickListener?, val baseView: View = LayoutInflater.from(viewGroup.context)
            .inflate(layoutId, viewGroup, false)
    ) : RecyclerView.ViewHolder(baseView) {
        val dateText: TextView
        val timeText: TextView
        val bubble: RelativeLayout
        val bubbtleSpace: LinearLayout
        val userNameView: TextView
        val quotationContainer: ViewGroup?


        init {
            quotationContainer = baseView.findViewById(R.id.item_quotation_container)
            userNameView = baseView.findViewById(R.id.item_username)
            dateText = baseView.findViewById(R.id.item_date)
            timeText = baseView.findViewById(R.id.item_time)
            bubble = baseView.findViewById(R.id.item_bubble)
            bubbtleSpace = baseView.findViewById(R.id.bubble_space)
            baseView.setOnClickListener {
                if (mClickListener != null) mClickListener!!.onItemClick(baseView, adapterPosition)
            }
        }
    }

    class AudioViewHolder(viewGroup: ViewGroup, mClickListener: ItemClickListener?) :
        BaseViewHolder(R.layout.activity_chat_window_list_audio_item, viewGroup, mClickListener) {
        val playButton: ImageButton
        val progressBar: ProgressBar


        init {
            // Define click listener for the ViewHolder's View.
            playButton = baseView.findViewById(R.id.audio_item_play_button)
            progressBar = baseView.findViewById(R.id.audio_item_progress)
        }
    }

    class ImageViewHolder(viewGroup: ViewGroup, mClickListener: ItemClickListener?) :
        BaseViewHolder(R.layout.activity_chat_window_list_image_item, viewGroup, mClickListener) {
        val imageButton: ImageButton

        init {
            // Define click listener for the ViewHolder's View.
            imageButton = baseView.findViewById(R.id.item_image)
        }
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class TextViewHolder(viewGroup: ViewGroup, mClickListener: ItemClickListener?) :
        BaseViewHolder(R.layout.activity_chat_window_list_text_item, viewGroup, mClickListener) {
        val textView: TextView

        init {
            // Define click listener for the ViewHolder's View.
            textView = baseView.findViewById(R.id.item_text)
        }
    }

    override fun getItemViewType(position: Int): Int {
        // Just as an example, return 0 or 2 depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous
        val decrypted = dataSet[position]
        when (decrypted.type) {
            MessageTypes.TEXT_MESSAGE.ordinal -> {
                return ViewHolderType.VIEWTYPE_TEXT_MESSAGE.ordinal
            }
            MessageTypes.ATTACHMENT_MESSAGE.ordinal -> {
                if (decrypted is AttachmentMessage) {
                    val attachment = decrypted.getAttachment()
                    Logging.d(TAG, "getItemViewType [+] mimetype ${attachment.mimetype}")
                    if (isSupportedAudio(attachment.mimetype)) {
                        return ViewHolderType.VIEWTYPE_AUDIO_MESSAGE.ordinal
                    } else if (isSupportedImage(attachment.mimetype)) {
                        return ViewHolderType.VIEWTYPE_IMAGE_MESSAGE.ordinal
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

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): BaseViewHolder {
        // Create a new view, which defines the UI of the list item

        // TODO message types !!
        return when (viewType) {
            ViewHolderType.VIEWTYPE_TEXT_MESSAGE.ordinal -> {
                TextViewHolder(viewGroup, mClickListener)
            }
            ViewHolderType.VIEWTYPE_AUDIO_MESSAGE.ordinal -> {
                AudioViewHolder(viewGroup, mClickListener)
            }
            ViewHolderType.VIEWTYPE_IMAGE_MESSAGE.ordinal -> {
                ImageViewHolder(viewGroup, mClickListener)
            }
            ViewHolderType.VIEWTYPE_ANY_ATTACHMENT_MESSAGE.ordinal -> {
                TextViewHolder(viewGroup, mClickListener)
            }
            else -> {
                TextViewHolder(viewGroup, mClickListener)
            }
        }
    }

    fun getCalendar(stamp: Long): Calendar {
        val c: Calendar = Calendar.getInstance()
        c.setTimeInMillis(stamp)
        return c
    }

    inner class AdapterAudioPlayer(val cert: Certificate?, val message: AttachmentMessage, var viewHolder: AudioViewHolder) : MediaManagerCallback {



        init {
            prepareUi(MediaManagerState.STOPPED)

            if(MediaManager.attachmentId == message.getAttachment().attachmentId) {
                prepareUi(MediaManager.state)
            }
            MediaManager.callbacks.add(this)
        }

        fun prepareUi(state: MediaManagerState) {
            Logging.d(TAG, "prepareUi [+] change state to <$state>")
            when (state) {
                MediaManagerState.PREPARING -> {
                    Handler(Looper.getMainLooper()).post {
                        viewHolder?.playButton?.visibility = View.GONE
                        viewHolder?.progressBar?.visibility = View.VISIBLE
                    }
                }
                MediaManagerState.PLAYING -> {
                    Handler(Looper.getMainLooper()).post {
                        viewHolder?.playButton?.visibility = View.VISIBLE
                        viewHolder?.progressBar?.visibility = View.GONE
                        viewHolder?.playButton?.setImageResource(R.drawable.baseline_stop_circle_white_24)
                    }
                    viewHolder?.playButton?.setOnClickListener {
                        MediaManager.state = MediaManagerState.STOPPED
                    }
                }
                MediaManagerState.STOPPED -> {
                    Handler(Looper.getMainLooper()).post {
                        viewHolder?.playButton?.visibility = View.VISIBLE
                        viewHolder?.progressBar?.visibility = View.GONE
                        if (message.getAttachment().isDownloaded(context)) {
                            viewHolder?.playButton?.setImageResource(R.drawable.baseline_play_circle_filled_white_24)
                        } else {
                            viewHolder?.playButton?.setImageResource(R.drawable.baseline_get_app_white_24)
                        }
                    }
                    viewHolder?.playButton?.setOnClickListener {
                        playAudioMessage()
                    }
                }
                MediaManagerState.ERROR -> {

                }
            }
        }

        fun playAudioMessage() {
            MediaManager.playAudioMessage(cert, message, context)
        }

        override fun onStateChanged(attachmentId: String?, state: MediaManagerState) {
            Logging.d(TAG, "onStateChanged [+] <$attachmentId, $state>")
            if(attachmentId == message.getAttachment().attachmentId) {
                prepareUi(state)
            }
        }
    }

    val players = HashMap<String, AdapterAudioPlayer>()


    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: BaseViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element

        // lazy loading
        checkMoreMessagesRequired(position)

        // content
        viewHolder.bubbtleSpace.setBackgroundColor(context.getColorFromAttr(R.attr.bubbleBackground))
        viewHolder.bubble.visibility = View.VISIBLE
        if (viewHolder is TextViewHolder)
            viewHolder.textView.text = ""
        //val encryptedMessage = dataSet[position]
        val message = dataSet[position]//decrypt(encryptedMessage) ?: InvalidMessage("<Decryption Error>")

        viewHolder.dateText.text = message.getCreationDateText()
        val current = getCalendar(message.created)

        if (position - 1 >= 0) {
            dataSet[position - 1].let { msgBefore ->
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
        }

        // time

        viewHolder.timeText.text = message.getCreationTimeText()

        if (showUserNames) {
            var userName = message.hashedFrom
            val user = UserManager.getUserByHashedId(message.hashedFrom).get()
            if (user != null) {
                val alias = user.getLastAlias()
                if (alias != null) {
                    userName = alias.alias
                }
            }
            viewHolder.userNameView.text = userName
            viewHolder.userNameView.visibility = View.VISIBLE
        }

        // color
        if (message is SymmetricMessage) {

            // quotation
            if(viewHolder.quotationContainer != null) {
                viewHolder.quotationContainer.visibility = View.GONE
                viewHolder.quotationContainer.removeAllViews()
                Logging.d(TAG, "quotation ${message.extra}")
                if(message.extra.isNotEmpty()) {
                    try {
                        val json = JSONObject(message.extra)
                        if(json.has(EXTRA_QUOTED_MESSAGE_ID)) {
                            val messageId = json.getString(EXTRA_QUOTED_MESSAGE_ID)
                            MessageManager.getMessageById(messageId).get()?.let {
                                decrypt(it)?.let {
                                    Logging.d(TAG, "quotation addview")

                                    val view = quotationViewManager.createQuotationView(it, viewHolder.quotationContainer)
                                    viewHolder.quotationContainer.addView(view)
                                    viewHolder.quotationContainer.visibility = View.VISIBLE
                                }
                            }
                        }
                    } catch (exception: Exception) {
                        Logging.e(TAG, "onBindViewholder [-] error while extract quotation data", exception)
                    }
                }
            }


            if (message is ITextMessage) {
                if (viewHolder is TextViewHolder) {
                    // url clicks
                    viewHolder.textView.text = message.getText().text // todo formatting
                    Linkify.addLinks(viewHolder.textView, Linkify.WEB_URLS);
                    viewHolder.textView.handleUrlClicks {
                        mClickListener?.onUrlClicked(it)
                    }

                    // quotation todo make generic !


                } else {
                    Logging.e(TAG, "onBindViewholder [-] invalid viewholder type set for text message ${viewHolder}")
                }
            } else if (message is AttachmentMessage) {
                when (viewHolder.itemViewType) {
                    ViewHolderType.VIEWTYPE_AUDIO_MESSAGE.ordinal -> {
                        if (viewHolder is AudioViewHolder) {

                            val attachmentId = message.getAttachment().attachmentId
                            if (!players.containsKey(attachmentId)) {
                                players[attachmentId] = AdapterAudioPlayer(getMessagePub(message), message, viewHolder)
                            }
                            players[attachmentId]?.viewHolder = viewHolder

                        } else {
                            Logging.e(TAG, "onBindViewholder [-] invalid viewholder type set ${viewHolder.itemViewType} : $viewHolder")
                        }
                    }
                    ViewHolderType.VIEWTYPE_IMAGE_MESSAGE.ordinal -> {
                        if (viewHolder is ImageViewHolder) {
                            val callback: (ByteArray?) -> Unit = {
                                Logging.d(TAG, "onBindViewHolder [+] got image bytes ${it?.size}")
                                if (it != null) {
//                                    val image = BitmapDrawable(BitmapFactory.decodeByteArray(it, 0, it.size))
                                    Handler(Looper.getMainLooper()).post {
                                        if (context is Activity) {
                                            if (context.isDestroyed) {
                                                return@post
                                            }
                                        }
                                        Glide.with(context).load(it).into(viewHolder.imageButton);
//                                        viewHolder.imageButton.setImageDrawable(image)
                                    }
                                    viewHolder.imageButton.setOnClickListener {
                                        // todo open image!?
                                        openImageViewerForMessage(message, context)
                                    }

                                } else {
                                    // todo
                                    Handler(Looper.getMainLooper()).post {
                                        viewHolder.imageButton.setImageResource(android.R.drawable.stat_notify_error)
                                    }
                                }
                            }

                            if (message.getAttachment().thumbnail != null && message.getAttachment().thumbnail!!.isNotEmpty()) {
                                callback(message.getAttachment().thumbnail)
                            } else if (message.getAttachment().isDownloaded(context)) {
                                callback(DownloadManager.getAttachmentBytes(getMessagePub(message), message, context))
                            } else {
                                viewHolder.imageButton.setImageResource(R.drawable.baseline_get_app_white_36)
                                viewHolder.imageButton.setOnClickListener {
                                    getAttachmentBytesAsync(getMessagePub(message), message, context, callback)
                                }
                            }
                        } else {
                            Logging.e(TAG, "onBindViewholder [-] invalid viewholder type set ${viewHolder.itemViewType} : $viewHolder")
                        }
                    }
                    else -> {
                        Logging.e(TAG, "onBindViewholder [-] unsupported attachment viewholder ${viewHolder.itemViewType}")
                    }
                }
            }


//            val top = viewHolder.bubbtleSpace.paddingTop
//            val bottom = viewHolder.bubbtleSpace.paddingBottom
//            val right = viewHolder.bubbtleSpace.paddingRight
//            val left = viewHolder.bubbtleSpace.paddingLeft
            if (IDGenerator.toHashedId(UserManager.myId!!) != message.hashedFrom) {
                val params = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                params.marginEnd = 100
                viewHolder.bubbtleSpace.layoutParams = params
                viewHolder.bubbtleSpace.setBackgroundColor(context.getColorFromAttr(R.attr.bubbleBackground_two))
//                viewHolder.bubbtleSpace.setPadding(100, top, right, bottom)

//                viewHolder.bubbtleSpace.gravity = Gravity.LEFT
                viewHolder.timeText.gravity = Gravity.LEFT

            } else {
                val params = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                params.marginStart = 100
                viewHolder.bubbtleSpace.layoutParams = params

                viewHolder.timeText.gravity = Gravity.RIGHT
            }


//            if(MessageStatus.hasFlag(dataSet[position].messageStatus, MessageStatus.NOT_SENT)) {
//                viewHolder.textView.setTextColor(Color.RED) // error
//            } else

            Logging.d(TAG, "onBindViewHolder [+] status <${message.messageId}, ${MessageStatus.dumpState(message.messageStatus)}>")
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
            if (viewHolder is TextViewHolder)
                viewHolder.textView.text = message.getText().text // todo formatting
        } else {
            if (viewHolder is TextViewHolder)
                viewHolder.textView.text = "UNSUPPORTED MESSAGE (" + dataSet[position].javaClass.simpleName + ")"
        }
    }

    fun checkMoreMessagesRequired(position: Int) {
        Logging.d(TAG, "checkMoreMessagesRequired [+] <$position, ${dataSet.size}>")
        if ((dataSet.size - position) >= dataSet.size / 2) { // todo half of dataset?
            onMoreMessagesRequiredListener?.let {
                it(position)
            }
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

    var onMoreMessagesRequiredListener: ((Int) -> Unit)? = null


    companion object {
        const val TAG = "ChatAdapter"
    }


    fun decrypt(encryptedMessage: EncryptedMessage): IMessage? {
        try {
            return MessageProcessor.unpack(encryptedMessage, getMessagePub(encryptedMessage), myKey)
        } catch (exception: Exception) {
            Logging.e(TAG, "Unable to decrypt message ${encryptedMessage.messageId}", exception)
            return InvalidMessage("<Decryption Error (${exception.message}>")
        }
    }

    fun getMessagePub(hashedFrom: String): Certificate? {
        var pub = partnerPub
        if (hashedFrom.equals(IDGenerator.toHashedId(UserManager.myId!!))) {
            pub = myPub
        }
        return pub // todo fix me
    }

    fun getMessagePub(encryptedMessage: EncryptedMessage): Certificate? {
        return getMessagePub(encryptedMessage.hashedFrom)
    }


    fun getMessagePub(message: SymmetricMessage): Certificate? {
        return getMessagePub(message.hashedFrom)
    }
}
