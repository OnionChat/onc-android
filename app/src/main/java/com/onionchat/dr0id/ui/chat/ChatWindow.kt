package com.onionchat.dr0id.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onionchat.common.*
import com.onionchat.dr0id.OnionChatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.database.BroadcastManager
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.MessageProcessor
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.dr0id.messaging.keyexchange.NegotiateSymKeyMessage
import com.onionchat.dr0id.messaging.messages.*
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.ui.contactdetails.ContactDetailsActivity
import com.onionchat.dr0id.ui.web.MovableWebWindow
import com.onionchat.localstorage.userstore.Conversation


open class ChatWindow : OnionChatActivity(), ChatAdapter.ItemClickListener {
    var conversation: Conversation? = null;

    var mText: String = ""; // TODO temporary !!
    lateinit var mChatView: RecyclerView;
    lateinit var mMessageInput: EditText;
    lateinit var mRootLayout: RelativeLayout;
    lateinit var mSendButton: ImageButton;

    var adapter: ChatAdapter? = null
    val messageList = ArrayList<IMessage>()
    lateinit var resultLauncher: ActivityResultLauncher<Intent>

    companion object {
        val EXTRA_PARTNER_ID = "partnerId"
        val TAG = "ChatWindow"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(FLAG_SPLIT_TOUCH, FLAG_SPLIT_TOUCH)
        super.onCreate(savedInstanceState)

        // UI
        setContentView(R.layout.activity_chat_window)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setOnClickListener {
            conversation?.user?.let {
                openContactDetails(it.id, resultLauncher)
            }
            conversation?.broadcast?.let {
                openBroadcastDetails(it.id, resultLauncher)
            }
        }
        mRootLayout = findViewById<RelativeLayout>(R.id.activity_chat_window_root)
        mMessageInput = findViewById(R.id.chat_window_message_enter)
        mSendButton = findViewById(R.id.chat_window_send_button)
        mChatView = findViewById(R.id.chat_window_message_display)
        mChatView.setLayoutManager(LinearLayoutManager(this));
        adapter = ChatAdapter(messageList)
        mChatView.adapter = adapter

        mSendButton.setOnClickListener {
            val text = mMessageInput.text.toString()
            mMessageInput.text.clear()
            sendTextMessage(text)
        }
//        mMessageInput.addTextChangedListener {
//            if(it.toString().contains("https://")) {
//                mMessageInput.setText(it.toString().replace("https://", "onionchat://"))
//            } else if (it.toString().contains("http://")){
//                mMessageInput.setText(it.toString().replace("http://", "onionchat://"))
//            }
//        }
        adapter?.setClickListener(this)


        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ContactDetailsActivity.USER_DELETED) {
                val deleted_uid = result.data?.getStringExtra(ContactDetailsActivity.EXTRA_CONTACT_ID)
                if (deleted_uid != null) {
                    finish()
                }
            }
        }

        if (getChatPartner() == null) {
            updateConnectionState(ConnectionStatus.ERROR)
        } else {
            loadMessages() // todo make async
        }
    }

    open fun getChatPartner(): Conversation? {
        intent.extras?.getString(EXTRA_PARTNER_ID)?.let {
            Logging.d("ChatWindow", "onCreate - access database")
            UserManager.getUserById(it).get()?.let {
                conversation = Conversation(it, 0, true)
            } ?: kotlin.run {
                BroadcastManager.getBroadcastById(it).get()?.let {
                    conversation = Conversation(null, 0, true, it)
                }
            }
            setTitle(conversation?.getLabel())
            Logging.d("ChatWindow", "Start chat with conversation <" + conversation + ">")
            return conversation
        }; // TODO accses via intent ?
        return null
    }

    fun loadMessages() {
        Thread {
            conversation?.let { conversation ->
                conversation.user?.certId?.let {
                    val partnerPub = Crypto.getPublicKey(it)
                    MessageManager.getAllForConversation(conversation).get()?.forEach {
                        if (it.type != MessageTypes.MESSAGE_READ_MESSAGE.ordinal) {
                            try {
                                var pub = partnerPub
                                if (it.hashedFrom.equals(IDGenerator.toHashedId(UserManager.myId!!))) {
                                    pub = Crypto.getMyPublicKey()
                                }
                                MessageProcessor.unpack(it, pub, Crypto.getMyKey())?.let {
                                    messageList.add(it)
                                }
                            } catch (exception: Exception) {
                                Logging.e(TAG, "Error while decrypt message $it", exception)
                                exception.message?.let {
                                    messageList.add(InvalidMessage(it))
                                }
                            }
                        }
                    }
                }
            }
            runOnUiThread {
                if (messageList.isNotEmpty()) {
                    adapter?.notifyDataSetChanged()
                    mChatView.scrollToPosition(messageList.size - 1)
                }
            }
        }.start()
    }

    override fun onReceiveMessage(message: IMessage): Boolean {
        Logging.d(TAG, "onReceiveMessage <$message>")

        if (conversation == null) {
            return false
        }

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
            return true
        } else if (message is TextMessage) {
            if (message.hashedFrom == IDGenerator.toHashedId(conversation!!.getId())) {
                sendMessageReadMessage(message)
                runOnUiThread {
                    val old = messageList.count();
                    messageList.add(message)
                    adapter?.notifyItemInserted(old)
                    mChatView.smoothScrollToPosition(old)
                }
                return true
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
                if (it is SymmetricMessage && it.signature == message.messageSignature) {
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

    override fun onConnected(success: Boolean) {
        if (!success) {
            updateConnectionState(ConnectionStatus.ERROR)
        } else {
            updateConnectionState(ConnectionStatus.CONNECTING)
            startRecursivePing()
        }
    }


    fun startRecursivePing(tries: Int = 4) {
        if (tries <= 0) {
            runOnUiThread {
                updateConnectionState(ConnectionStatus.ERROR)
            }
            return
        } else {
            conversation?.user?.let {
                ConnectionManager.isUserOnline(it) {
                    if (it) {
                        runOnUiThread {
                            updateConnectionState(ConnectionStatus.CONNECTED)
                        }
                    } else {
                        startRecursivePing(tries - 1)
                    }
                }
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
            R.id.ping_contact -> {
                startRecursivePing()
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


    fun sendMessageReadMessage(message: IMessage) {
        if (message is SymmetricMessage) {
            conversation?.user?.let { // todo broadcast ?
                if (SettingsManager.getBooleanSetting(getString(R.string.key_enable_message_read), this)) {
                    Logging.d(TAG, "send message read message <" + message.signature + ">")

                    val readMessage = MessageReadMessage(
                        messageSignature = message.signature,
                        hashedFrom = IDGenerator.toHashedId(UserManager.myId!!),
                        hashedTo = message.hashedFrom
                    )// TODO ?
                    sendMessage(readMessage, UserManager.myId!!, it) // TODO ?
                    //Communicator.sendMessage(it.id, MessageProcessor.encodeMessage(message, it.certId))
                } else {
                    Logging.d(TAG, "message read is disabled")
                }
            }
        }
    }

    fun sendTextMessage(text: String) {
        var listId = 0;

        conversation?.user?.let {
            var message = TextMessage(
                textData = TextMessageData(text, ""), // todo add format info
                hashedFrom = IDGenerator.toHashedId(UserManager.myId!!),
                hashedTo = it.getHashedId()
            )
            sendMessage(message, UserManager.myId!!, it)?.then {
                if (it.status == OnionTask.Status.SUCCESS) {
                    message.messageStatus = MessageStatus.addFlag(message.messageStatus, MessageStatus.SENT)
                }
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

            runOnUiThread {
                listId = messageList.size
                messageList.add(message)
                adapter?.notifyItemInserted(listId)
                mChatView.smoothScrollToPosition(listId)
            }
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
        val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        conversation?.getLabel()?.let {
            if (messageList[position] is ITextMessage) {
                val textMessage = messageList[position] as ITextMessage
                val clip = ClipData.newPlainText(it, textMessage.getText().text)
                clipboard.setPrimaryClip(clip)
            }

        }

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
}