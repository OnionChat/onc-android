package com.onionchat.dr0id.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.common.SettingsManager
import com.onionchat.connector.Communicator
import com.onionchat.dr0id.OnionChatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.messaging.MessagePacker
import com.onionchat.dr0id.messaging.messages.BroadcastTextMessage
import com.onionchat.dr0id.messaging.messages.Message
import com.onionchat.dr0id.messaging.messages.MessageReadMessage
import com.onionchat.dr0id.messaging.messages.TextMessage
import com.onionchat.dr0id.ui.contactdetails.ContactDetailsActivity
import com.onionchat.dr0id.ui.web.MovableWebWindow
import com.onionchat.dr0id.ui.web.OnionWebClient
import com.onionchat.dr0id.users.BroadcastManager
import com.onionchat.dr0id.users.UserManager
import com.onionchat.localstorage.userstore.Conversation
import com.onionchat.localstorage.userstore.User
import java.lang.Thread.sleep


open class ChatWindow : OnionChatActivity(), ChatAdapter.ItemClickListener {
    var conversation: Conversation? = null;

    var mText: String = ""; // TODO temporary !!
    lateinit var mChatView: RecyclerView;
    lateinit var mMessageInput: EditText;
    lateinit var mSendButton: ImageButton;

    var adapter: ChatAdapter? = null
    val messageList = ArrayList<Message>()
    lateinit var resultLauncher: ActivityResultLauncher<Intent>

    companion object {
        val EXTRA_PARTNER_ID = "partnerId"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
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

    override fun onReceiveMessage(message: Message): Boolean {
        if (conversation == null) {
            return false
        }
        if (message.from.equals(IDGenerator.toVisibleId(conversation!!.getId()))) {

            if (message is BroadcastTextMessage) {
                if (conversation?.broadcast != null) {
                    if (conversation?.broadcast!!.id == message.broadcastId) {
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
                sendMessageReadMessage(message.signature)
                runOnUiThread {
                    val old = messageList.count();
                    messageList.add(message)
                    adapter?.notifyItemInserted(old)
                    mChatView.smoothScrollToPosition(old)
                }
                return true

            } else if (message is MessageReadMessage) {
                Logging.d("ChatWindow", "Received message read message <" + message.messageSignature + ">")
                messageList.forEachIndexed { i, it ->
                    if (IDGenerator.toVisibleId(it.signature) == message.messageSignature) {
                        it.read = true
                        runOnUiThread {
                            adapter?.notifyItemChanged(i)
                        }
                    }
                }
                return true

            }
        }
        // unknown message
        return false
    }

    override fun onConnected(success: Boolean) {
        if (!success) {
            updateConnectionState(ConnectionStatus.ERROR)
        } else {
            startRecursivePing()
        }
    }

    var noTests = 10

    fun startRecursivePing() {
        conversation?.user?.let {
            updateConnectionState(ConnectionStatus.CONNECTING)
            noTests = 10
            recursivePing(it)
        }
    }

    fun recursivePing(user: User) {

        ConnectionManager.isUserOnline(user) {
            if (!it) {
                sleep(1000)
                noTests -= 1
                if (noTests == 0) {
                    runOnUiThread {
                        updateConnectionState(ConnectionStatus.ERROR)
                    }
                } else {
                    recursivePing(user)
                }
            } else {
                runOnUiThread {
                    updateConnectionState(ConnectionStatus.CONNECTED)
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
            else -> {
            }
        }
        return true
    }


    fun sendMessageReadMessage(signatureStr: String) {
        conversation?.user?.let {
            if (SettingsManager.getBooleanSetting(getString(R.string.key_enable_message_read), this)) {
                Logging.d("ChatWindow", "send message read message <" + signatureStr + ">")
                val message = MessageReadMessage(IDGenerator.toVisibleId(signatureStr), UserManager.myId!!)
                Communicator.sendMessage(it.id, MessagePacker.encodeMessage(message, it.certId))
            } else {
                Logging.d("ChatWindow", "message read is disabled")
            }
        }

    }

    fun sendTextMessage(text: String) {
        var listId = 0;
        var message = TextMessage(text, UserManager.myId!!)
        conversation?.user?.let {
            Communicator.sendMessage(it.id, MessagePacker.encodeMessage(message, it.certId)) {
                message.status = it
                runOnUiThread {
                    if (listId != 0) {
                        adapter?.notifyItemChanged(listId)
                    } else {
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
        conversation?.broadcast?.let {
            message = BroadcastTextMessage(it.id, it.label, text, UserManager.myId!!)
            Thread {
                // do that asynchronous
                UserManager.getAllUsers().get()?.forEach { // todo send this only to users added to the broadcast
                    Communicator.sendMessage(it.id, MessagePacker.encodeMessage(message, it.certId)) {
                        message.status = it
                        runOnUiThread {
                            if (listId != 0) {
                                adapter?.notifyItemChanged(listId)
                            } else {
                                adapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }.start()
        }



        runOnUiThread {
            listId = messageList.size
            messageList.add(message)
            adapter?.notifyItemInserted(listId)
            mChatView.smoothScrollToPosition(listId)
        }
    }

    override fun onItemClick(view: View?, position: Int) {
        val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        conversation?.getLabel()?.let {
            if (messageList[position] is TextMessage) {
                val textMessage = messageList[position] as TextMessage
                val clip = ClipData.newPlainText(it, textMessage.getText())
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

        val window = MovableWebWindow(mChatView, this, url)
        window.show()
    }
}