package com.onionchat.dr0id.ui.chat

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gsconrad.richcontentedittext.RichContentEditText
import com.onionchat.common.*
import com.onionchat.common.DateTimeHelper.timestampToString
import com.onionchat.common.MessageTypes.Companion.shouldBeShownInChat
import com.onionchat.dr0id.OnionChatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.database.*
import com.onionchat.dr0id.database.MessageManager.setMessageRead
import com.onionchat.dr0id.database.UserManager.getLastSeen
import com.onionchat.dr0id.media.stream.StreamController
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.IMessage.Companion.EXTRA_QUOTED_MESSAGE_ID
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.dr0id.messaging.data.Attachment
import com.onionchat.dr0id.messaging.keyexchange.NegotiateSymKeyMessage
import com.onionchat.dr0id.messaging.messages.*
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.tasks.CheckConnectionTask
import com.onionchat.dr0id.queue.tasks.ProcessPendingTask
import com.onionchat.dr0id.ui.contactdetails.ContactDetailsActivity
import com.onionchat.dr0id.ui.contentresolver.ContentResolverHelper.readFromUri
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer.showError
import com.onionchat.dr0id.ui.web.MovableWebWindow
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.userstore.Broadcast
import kotlinx.atomicfu.atomic
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.cert.Certificate
import java.util.*
import kotlin.collections.ArrayList


open class ChatWindow : OnionChatActivity(), ChatAdapter.ItemClickListener {
    var conversation: Conversation? = null;

    var mText: String = ""; // TODO temporary !!
    lateinit var progressBar: ProgressBar;
    lateinit var mChatView: RecyclerView;
    lateinit var mMessageInput: RichContentEditText;
    lateinit var mRootLayout: ViewGroup;
    lateinit var mQuotationContainer: ViewGroup;
    lateinit var mSendButton: ImageButton;

    var adapter: ChatAdapter? = null
    val messageList = ArrayList<IMessage>()
    lateinit var resultLauncher: ActivityResultLauncher<Intent>
    var quotationViewManager = QuotationViewManager(this)

    var currentQuotedMesageId: String? = null

    enum class QuotationViewState {
        VISIBLE,
        INVISIBLE
    }

    var quotationViewState = QuotationViewState.INVISIBLE
        set(it) {
            when (it) {
                QuotationViewState.VISIBLE -> {
                    mQuotationContainer.visibility = View.VISIBLE
                }
                QuotationViewState.INVISIBLE -> {
                    runOnUiThread{
                        mQuotationContainer.visibility = View.INVISIBLE
                        mQuotationContainer.removeAllViews()
                        currentQuotedMesageId = null
                    }

                }
            }
            field = it
        }

    companion object {
        val EXTRA_PARTNER_ID = "partnerId"
        val TAG = "ChatWindow"
    }

    enum class SendButtonState {
        ABLE_TO_SEND_MESSAGE,
        ABLE_TO_RECORD,
        ABLE_TO_SEND_RECORD
    }

    var sendButtonState = SendButtonState.ABLE_TO_RECORD
        set(it) {
            Logging.d(TAG, "change send button state to ${it}")
            when (it) {
                SendButtonState.ABLE_TO_RECORD -> {
                    mSendButton.setImageResource(R.drawable.baseline_mic_black_36)
                    mMessageInput.isEnabled = true
                }
                SendButtonState.ABLE_TO_SEND_MESSAGE -> {
                    mSendButton.setImageResource(android.R.drawable.ic_menu_send)
                }
                SendButtonState.ABLE_TO_SEND_RECORD -> {
                    mSendButton.setImageResource(R.drawable.baseline_stop_black_36)
                    mMessageInput.isEnabled = false
                }
            }
            field = it
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(FLAG_SPLIT_TOUCH, FLAG_SPLIT_TOUCH)
        super.onCreate(savedInstanceState)

        // UI
        setContentView(R.layout.activity_chat_window)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true);
        toolbar.setOnClickListener {
            conversation?.user?.let {
                openContactDetails(it.id, resultLauncher)
            }
            conversation?.broadcast?.let {
                openBroadcastDetails(it.id, resultLauncher)
            }
        }
        mQuotationContainer = findViewById(R.id.chat_window_quotation)
        mRootLayout = findViewById(R.id.activity_chat_window_root)
        mMessageInput = findViewById(R.id.chat_window_message_enter)
        mSendButton = findViewById(R.id.chat_window_send_button)
        mChatView = findViewById(R.id.chat_window_message_display)
        progressBar = findViewById(R.id.chat_window_progress)
        val linearLayoutManager = LinearLayoutManager(this)
//        linearLayoutManager.setReverseLayout(true);
//        linearLayoutManager.setStackFromEnd(true);
        mChatView.setLayoutManager(linearLayoutManager);


        // get chat partner information
        if (getChatPartner() == null) {
            updateConnectionState(ConnectionManager.ConnectionState.ERROR)
        }
        var partnerPub: Certificate? = null
        conversation?.user?.certId?.let {
            partnerPub = Crypto.getPublicKey(it)
        } // todo broadcast ?

        var showUsernames = false
        conversation?.let {
            if (it.getConversationType() == ConversationType.BROADCAST) {
                showUsernames = true
            }
        }

        // setup adapter for decryption
        adapter = ChatAdapter(messageList, partnerPub, Crypto.getMyPublicKey(), context = this@ChatWindow, showUserNames = showUsernames)
        mChatView.adapter = adapter

//        val simpleCallback: ItemTouchHelper.SimpleCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {
//            override fun onMove(
//                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
//                target: RecyclerView.ViewHolder
//            ): Boolean {
//                Logging.d(TAG, "onMove [+] <${viewHolder}, $target>")
//
//                return false
//            }
//
//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                //do things
//                Logging.d(TAG, "onSwiped [+] <${viewHolder}, $direction>")
//            }
//        }
//        val callback : ItemTouchHelper.Callback = object : ItemTouchHelper.Callback() {
//            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
//                return ItemTouchHelper.Callback.makeMovementFlags(0, ItemTouchHelper.RIGHT)
//            }
//
//            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
//                Logging.d(TAG, "onMove [+] <${viewHolder}, $target>")
//                return false
//            }
//
//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                Logging.d(TAG, "onSwiped [+] <${viewHolder}, $direction>")
//            }
//
//            override fun onChildDraw(
//                c: Canvas,
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                dX: Float,
//                dY: Float,
//                actionState: Int,
//                isCurrentlyActive: Boolean
//            ) {
////                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
////                if (actionState == ACTION_STATE_SWIPE) {
////                    setTouchListener(recyclerView, viewHolder)
////                }
//
//                if (mView.translationX < convertTodp(130) || dX < this.dX) {
//                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
//                    this.dX = dX
//                    startTracking = true
//                }
//                currentItemViewHolder = viewHolder
//            }
//        }

        val itemTouchHelper = ItemTouchHelper(MessageSwipeController(this, object : SwipeControllerActions {
            override fun showReplyUI(position: Int) {
                val message = messageList[position]
                val view = quotationViewManager.createQuotationView(message, mQuotationContainer)
                if (view == null) {
                    Logging.e(TAG, "showReplyUI [-] error while create quotation view... abort")
                    return
                }
                runOnUiThread {
                    currentQuotedMesageId = message.messageId
                    mQuotationContainer.addView(view)
                    quotationViewState = QuotationViewState.VISIBLE
                }
            }
        }))

        itemTouchHelper.attachToRecyclerView(mChatView)

        mSendButton.setOnClickListener {
            when (sendButtonState) {
                SendButtonState.ABLE_TO_RECORD -> {
                    if (!startAudioRecord()) {
                        Logging.e(TAG, "onCreate [-] unable to record audio")
                    }
                    sendButtonState = SendButtonState.ABLE_TO_SEND_RECORD
                }
                SendButtonState.ABLE_TO_SEND_MESSAGE -> {
                    val text = mMessageInput.text.toString()
                    mMessageInput.text?.clear()
                    sendTextMessage(text)
                    sendButtonState = SendButtonState.ABLE_TO_RECORD
                }
                SendButtonState.ABLE_TO_SEND_RECORD -> {
                    stopAndSendAudioRecord()
                    sendButtonState = SendButtonState.ABLE_TO_RECORD
                }
            }
        }
        mMessageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (mMessageInput.text.toString().isEmpty()) {
                    sendButtonState = SendButtonState.ABLE_TO_RECORD
                } else {
                    sendButtonState = SendButtonState.ABLE_TO_SEND_MESSAGE
                }
            }
        })
        mMessageInput.setOnRichContentListener { contentUri, description ->

            // Called when a keyboard sends rich content
            if (description.mimeTypeCount > 0) {
                val mimetype: String = description.getMimeType(0)
//                val fileExtension = MimeTypeMap.getSingleton()
//                    .getExtensionFromMimeType(mimetype)
//                val filename = "filenameGoesHere.$fileExtension"
//                val richContentFile = File(filesDir, filename)
//                if (!writeToFileFromContentUri(richContentFile, contentUri)) {
//                    Toast.makeText(
//                        this@MainActivity,
//                        R.string.rich_content_copy_failure, Toast.LENGTH_LONG
//                    ).show()
//                } else {
//                    val displayView = findViewById<WebView>(R.id.display_view)
//                    displayView.loadUrl("file://" + richContentFile.getAbsolutePath())
//                }
                Logging.d(TAG, "setOnRichContentListener [+] found URI $contentUri $mimetype")
                readFromUri(this, contentUri)?.let {
                    if (it.size > 0) {
                        Attachment.create(this, it, null, mimetype)?.let {
                            sendAttachmentMessage(it)
                        } ?: run {
                            showError(this, getString(R.string.unable_to_send_attachment), ErrorViewer.ErrorCode.RICH_CONTENT_ATTACHMENT_CREATE)
                        }
                    } else {
                        showError(this, getString(R.string.unable_to_send_attachment), ErrorViewer.ErrorCode.RICH_CONTENT_READ_URI_EMPTY)
                    }
                }
            }
        }


        adapter?.setClickListener(this)
        adapter?.onMoreMessagesRequiredListener = {
            loadMessages(position = it)
        }

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ContactDetailsActivity.USER_DELETED) {
                val deleted_uid = result.data?.getStringExtra(ContactDetailsActivity.EXTRA_CONTACT_ID)
                if (deleted_uid != null) {
                    finish()
                }
            }
        }

        loadMessages(limit = resources.getInteger(R.integer.lazy_loading_start_limit), toBottom = true) // todo make async
    }

    open fun getChatPartner(): Conversation? {
        intent.extras?.getString(EXTRA_PARTNER_ID)?.let {
            Logging.d(TAG, "onCreate - access database")
            UserManager.getUserById(it).get()?.let {
                conversation = Conversation(it, 0, true)
                getLastSeen(it).get()?.let {
                    supportActionBar?.subtitle = timestampToString(it.timestamp)
                }
            } ?: kotlin.run {
                BroadcastManager.getBroadcastById(it).get()?.let {
                    conversation = Conversation(null, 0, true, it)
                }
            }
            setTitle(conversation?.getLabel())


            Logging.d(TAG, "Start chat with conversation <" + conversation + ">")
            return conversation
        }; // TODO accses via intent ?
        return null
    }

    var currentOffset = 0;

    enum class LAZY_LOADING_STATE {
        LOADING,
        WAITING
    }

    var lazyLoadingState = atomic(LAZY_LOADING_STATE.WAITING)

    fun loadMessages(
        offset: Int = currentOffset,
        limit: Int = resources.getInteger(R.integer.lazy_loading_default_limit),
        toBottom: Boolean = false,
        position: Int? = null
    ) {
        Logging.d(TAG, "loadMessages [+] <$offset,$limit>")
        when (lazyLoadingState.value) {
            LAZY_LOADING_STATE.LOADING -> {
                Logging.d(TAG, "loadMessages [-] already loading.. abort")
                return@loadMessages
            }
        }
        lazyLoadingState.lazySet(LAZY_LOADING_STATE.LOADING)
        Thread {
            var inserted = 0
            val toBeAdded = ArrayList<IMessage>()
            conversation?.let { conversation ->
                conversation.getCertId()?.let {
                    MessageManager.getRangeForConversation(conversation, offset, limit).get()?.forEach { message ->
                        if (shouldBeShownInChat(message.type)) { // todo make central somehow
                            adapter?.decrypt(message)?.let {
                                toBeAdded.add(0, it)
                                inserted += 1
                            }
                        }
                        MessageManager.isFromMe(message)?.let {
                            if (!it) {
                                if (!MessageStatus.hasFlag(message.messageStatus, MessageStatus.READ)) {
                                    sendMessageReadMessage(message)
                                }
                            }
                        }
                    }
                }
            }
            currentOffset += inserted
            lazyLoadingState.lazySet(LAZY_LOADING_STATE.WAITING)

            runOnUiThread {
                if (toBeAdded.isNotEmpty()) {
                    messageList.addAll(0, toBeAdded)
                    adapter?.notifyItemRangeInserted(0, inserted)
                    if (toBottom) {
                        mChatView.scrollToPosition(messageList.size - 1)
                        progressBar.visibility = View.GONE
                    } else {
                        position?.let {
                            Logging.d(TAG, "loadMessages [+] scroll to position ${it + inserted}")
                            //mChatView.scrollToPosition(it + inserted)
                        }
                    }
//                    mRecyclerView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//                        override fun onGlobalLayout() {
//                            mRecyclerView.scrollToPosition(position)
//                            mRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
//                        }
//                    })
                }
            }
        }.start()
    }

    override fun onReceiveMessage(message: IMessage?, encryptedMessage: EncryptedMessage): Boolean {
        Logging.d(TAG, "onReceiveMessage <$message>")
        val conversation = conversation ?: return false


        if (message is BroadcastTextMessage) {
            if (conversation?.broadcast != null) {
                if (conversation?.broadcast!!.id == message.getBroadcast().id) {
                    runOnUiThread {
                        val old = messageList.count();
                        messageList.add(message)
                        adapter?.notifyItemInserted(old)
                        mChatView.smoothScrollToPosition(old)
                    }
                }
            }
            return isTopActivity
        } else if (message is TextMessage || message is AttachmentMessage) {
            if ((conversation.getConversationType() == ConversationType.BROADCAST && message.hashedTo == conversation.getHashedId()) ||
                conversation.getConversationType() == ConversationType.CHAT && message.hashedFrom == conversation.getHashedId()
            ) {
                sendMessageReadMessage(message)
                runOnUiThread {
                    val old = messageList.count();
                    messageList.add(message)
                    adapter?.notifyItemInserted(old)
                    mChatView.smoothScrollToPosition(old)
                }
                return isTopActivity
            } else {
                Logging.d(TAG, "onReceiveMessage [+] ignore message $encryptedMessage / $message")
                return false
            }

        } else if (message is NegotiateSymKeyMessage) {
            if (message.hashedFrom == IDGenerator.toHashedId(conversation!!.getId())) {
                runOnUiThread {
                    val old = messageList.count();
                    messageList.add(message)
                    adapter?.notifyItemInserted(old)
                    mChatView.smoothScrollToPosition(old)
                }
            }
        } else if (message is MessageReadMessage) {
            Logging.d(TAG, "Received message read message <" + message.messageSignature + ">")
            messageList.forEachIndexed { i, it ->
                if (it.signature == message.messageSignature) {
                    //it.read = true
                    Logging.d(TAG, "Update status of message <" + it.messageId + ">")

                    it.messageStatus = MessageStatus.addFlag(it.messageStatus, MessageStatus.READ) // todo add logic to message read task
                    // todo update encrypted message ?
                    runOnUiThread {
                        adapter?.notifyItemChanged(i)
                    }
                }
            }
            return true

        }

        // unknown message
        return false
    }

    override fun onCheckConnectionFinished(status: CheckConnectionTask.CheckConnectionResult) {
        if (status.status != OnionTask.Status.SUCCESS) {
            updateConnectionState(ConnectionManager.ConnectionState.ERROR)
        } else {
            updateConnectionState(ConnectionManager.ConnectionState.CONNECTED)
            conversation?.user?.let {
                startRecursivePing(user = it)
            }
        }
    }

    override fun onConnectionStateChanged(state: ConnectionManager.ConnectionState) {
        updateConnectionState(state)
        conversation?.user?.let {
            startRecursivePing(user = it)
        }
    }

    override fun onConversationOnline(online: Boolean) {
        super.onConversationOnline(online)
        conversation?.user?.let {
            OnionTaskProcessor.enqueue(ProcessPendingTask(it)) // todo only messages of this user
        }
        if(online) {
            runOnUiThread {
                supportActionBar?.subtitle = getString(R.string.onilne)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // retrieve connection status
        checkConnection()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_window_menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        when (id) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.ping_contact -> {
                conversation?.user?.let {
                    startRecursivePing(user = it)
                }
            }
            R.id.open_web -> {
                conversation?.user?.let {
                    openContactWebSpace(it)
                }
            }
            R.id.open_stream -> {
                conversation?.user?.let {
                    openStreamWindow(it)
                }
            }
            else -> {
            }
        }
        return true
    }

    fun sendMessageReadMessage(hashedFrom: String, signature: String) {
        conversation?.user?.let { it -> // todo broadcast ?
            if (SettingsManager.getBooleanSetting(getString(R.string.key_enable_message_read), this)) {
                Logging.d(TAG, "send message read message <" + signature + ">")

                val readMessage = MessageReadMessage(
                    messageSignature = signature,
                    hashedFrom = IDGenerator.toHashedId(UserManager.myId!!),
                    hashedTo = hashedFrom
                )// TODO ?
                sendMessage(readMessage, UserManager.myId!!, it)?.then {
                    it.sendingFuture?.then {
                        Logging.d(TAG, "sent message read message <" + it.status + ">")

                    }

                    if (it.status != OnionTask.Status.FAILURE) {
                        Logging.d(TAG, "prepare message read message <" + signature + "> succeeded <${it.status}>")
                        setMessageRead(readMessage)
                    } else {
                        Logging.e(TAG, "prepare message read message <" + signature + "> failed <${it.status}>")
                    }
                }
                //Communicator.sendMessage(it.id, MessageProcessor.encodeMessage(message, it.certId))
            } else {
                Logging.d(TAG, "message read is disabled")
            }
        }
    }

    fun sendMessageReadMessage(message: EncryptedMessage) {
        sendMessageReadMessage(message.hashedFrom, message.signature)
    }

    fun sendMessageReadMessage(message: IMessage) {
        if (message is SymmetricMessage) {
            sendMessageReadMessage(message.hashedFrom, message.signature)
        }
    }

    fun sendTextMessage(text: String) {
        var listId = 0;

        conversation?.let {
            var extra = ""
            if (it.getConversationType() == ConversationType.BROADCAST) {
                extra = Broadcast.createPayload(it.broadcast!!)
            }
            if (quotationViewState == QuotationViewState.VISIBLE && currentQuotedMesageId != null) {
                val json = if (extra.isEmpty()) JSONObject() else JSONObject(extra)
                json.put(EXTRA_QUOTED_MESSAGE_ID, currentQuotedMesageId)
                extra = json.toString() // todo centralize in IMessage
                quotationViewState = QuotationViewState.INVISIBLE
            }
            var message = TextMessage(
                textData = TextMessageData(text, ""), // todo add format info
                hashedFrom = UserManager.getMyHashedId()!!,
                hashedTo = it.getHashedId(),
                extra = extra
            )
            sendMessage(message, UserManager.myId!!, it)?.then { result ->
                result.sendingFuture?.then {
                    if (it.status == OnionTask.Status.SUCCESS) {
                        result.encryptedMessage?.let {
                            it.messageStatus = MessageStatus.addFlag(it.messageStatus, MessageStatus.SENT)
                            runOnUiThread {
                                adapter?.notifyItemChanged(listId)
                            }
                        }
                    }
                }
            }
            runOnUiThread {
                listId = messageList.size
                messageList.add(message) // todo shell we keep it here? Or shall we add the message immedately?
                adapter?.notifyItemInserted(listId)
                mChatView.smoothScrollToPosition(listId)
            }
//            Communicator.sendMessage(it.id, MessageProcessor.pack(message, it.certId)) {
//                message.status = it
//                runOnUiThread {
//                    if (listId != 0) {
//                        adapter?.notifyItemChanged(listId)
//                    } else {
//                        adapter?.notifyDataSetChanged()
//                    }
//                }
//            }


        }
//        conversation?.broadcast?.let { // todo handle broadcast ?
//            message = BroadcastTextMessage(it.id, it.label, text, UserManager.myId!!)
//            Thread {
//                // do that asynchronous
//                BroadcastManager.getBroadcastUsers(it).get()?.forEach { // todo send this only to users added to the broadcast
//                    Communicator.sendMessage(it.id, MessageProcessor.pack(message, it.certId)) {
//                        message.status = it
//                        runOnUiThread {
//                            if (listId != 0) {
//                                adapter?.notifyItemChanged(listId)
//                            } else {
//                                adapter?.notifyDataSetChanged()
//                            }
//                        }
//                    }
//                }
//            }.start()
//    }


    }

    override fun onItemClick(view: View?, position: Int) {
//        val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        conversation?.getLabel()?.let {
//            if (messageList[position] is ITextMessage) {
//                val textMessage = messageList[position] as ITextMessage
//                val clip = ClipData.newPlainText(it, textMessage.getText().text)
//                clipboard.setPrimaryClip(clip)
//            }
//
//        }
        // todo more options
    }

    override fun onUrlClicked(url: String) {
        //openOnionLinkInWebView(url)
        openWebViewDialog(url)
    }

    fun openWebViewDialog(url: String) {
//        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
//// ...Irrelevant code for customizing the buttons and title
//// ...Irrelevant code for customizing the buttons and title
//        //dialogBuilder.setTitle(url);
//        val inflater = LayoutInflater.from(this@ChatWindow)
//        val dialogView: View = inflater.inflate(R.layout.webview_dialog, null)
//        dialogBuilder.setView(dialogView)
//
//        val webview = dialogView.findViewById<WebView>(R.id.webview_dialog_webview)
//        webview?.let {
//            it.webViewClient = OnionWebClient()
//            it.webChromeClient = WebChromeClient()
//            it.getSettings().setDomStorageEnabled(true);
//            it.settings.javaScriptEnabled = true
//
//            it.loadUrl(url)
//        }
//        val alertDialog: AlertDialog = dialogBuilder.create()
//        alertDialog.show()
//        val window = MovableWebWindow(this, url)
//        window.open()


        /// BOTTOM SHEEETTT
//        val bottomSheetDialog = BottomSheetDialog(this)
//        bottomSheetDialog.setCanceledOnTouchOutside(false)
//        bottomSheetDialog.behavior
//        bottomSheetDialog.setContentView(R.layout.webview_dialog)
//
//        val webview = bottomSheetDialog.findViewById<WebView>(R.id.webview_dialog_webview)
//        webview?.let {
//            it.webViewClient = OnionWebClient()
//            it.webChromeClient = WebChromeClient()
//            it.getSettings().setDomStorageEnabled(true);
//            it.settings.javaScriptEnabled = true
//
//            it.loadUrl(url)
//        }
//
//        bottomSheetDialog.show()

        val window = MovableWebWindow(mRootLayout, this, url)
        window.show()
    }


    ////////////////// audio
    val controller = StreamController()
    var audioRecordOutputStream = ByteArrayOutputStream()

    fun startAudioRecord(): Boolean {

        val status = controller.startAudioStreamAsync { data, size ->
            if (size < 0) {
                Logging.e(TAG, "startAudioRecord [-] error while retrieve audio bytes ${size}")
                return@startAudioStreamAsync false
            }
            audioRecordOutputStream.write(data, 0, size)
            true
        }
        return status
    }

    fun sendAttachmentMessage(attachment: Attachment) {
        UserManager.myId?.let { myId ->
            conversation?.let { conversation ->
                var extra = ""
                if (conversation.getConversationType() == ConversationType.BROADCAST) {
                    extra = Broadcast.createPayload(conversation.broadcast!!)
                }
                if (quotationViewState == QuotationViewState.VISIBLE && currentQuotedMesageId != null) {
                    val json = if (extra.isEmpty()) JSONObject() else JSONObject(extra)
                    json.put(EXTRA_QUOTED_MESSAGE_ID, currentQuotedMesageId)
                    extra = json.toString()
                    quotationViewState = QuotationViewState.INVISIBLE
                } // todo move into one function
                val message = AttachmentMessage(
                    attachment = attachment, hashedFrom = IDGenerator.toHashedId(myId), hashedTo = conversation.getHashedId(), extras = extra
                )
                var listId = 0

                sendMessage(
                    message,
                    myId,
                    conversation
                )?.then {
                    if (it.status != OnionTask.Status.FAILURE) {

                        if(it.encryptedMessage == null) {
                            Logging.e(TAG, "sendAttachmentMessage [-] Unexpected error: no encrypted message found!")
                            showError(this, getString(R.string.unable_to_send_attachment), ErrorViewer.ErrorCode.ATTACHMENT_PREPARE_MISSING)
                        }
                    } else {
                        Logging.e(TAG, "sendAttachmentMessage [-] error while prepare message")
                        showError(this, getString(R.string.unable_to_send_attachment), ErrorViewer.ErrorCode.ATTACHMENT_PREPARE)
                    }
                }
                runOnUiThread {
                    listId = messageList.size
                    messageList.add(message)
                    adapter?.notifyItemInserted(listId)
                    mChatView.smoothScrollToPosition(listId)
                }
                return@sendAttachmentMessage
            } ?: run {
                Logging.e(TAG, "sendAttachmentMessage [-] unable to retrieve user")
            }
        } ?: run {
            Logging.e(TAG, "sendAttachmentMessage [-] unable create attachment metadata")
        }
        showError(this, getString(R.string.unable_to_send_attachment), ErrorViewer.ErrorCode.UNEXPECTED)
    }

    fun stopAndSendAudioRecord() {
        Thread {
            if(controller.status) {
                controller.stop()
                val data = audioRecordOutputStream.toByteArray()
                if(data.size == 0) {
                    Logging.e(TAG, "stopAndSendAudioRecord [-] recorded audio bytes are empty")
                    return@Thread
                }
                Logging.d(TAG, "stopAndSendAudioRecord [+] <${data.size}>")
                audioRecordOutputStream.reset()
                // todo put this into a task !!
                Attachment.create(this, data, null,"audio/wav")?.let { attachment -> // audio pcm ?
                    sendAttachmentMessage(attachment)
                } ?: kotlin.run {
                    showError(this, getString(R.string.unable_to_send_attachment), ErrorViewer.ErrorCode.AUDIO_ATTACHMENT_CREATE)
                }
            }

        }.start()
    }

    override fun onPause() {
        super.onPause()
        stopAndSendAudioRecord()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}