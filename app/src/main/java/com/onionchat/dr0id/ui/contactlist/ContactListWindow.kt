package com.onionchat.dr0id.ui.contactlist

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import com.onionchat.common.Crypto
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.common.QrPayload
import com.onionchat.connector.BackendConnector
import com.onionchat.dr0id.OnionChatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.messaging.messages.IBroadcastMessage
import com.onionchat.dr0id.messaging.messages.Message
import com.onionchat.dr0id.qr.QrGenerator
import com.onionchat.dr0id.ui.broadcast.CreateBroadCastActivity
import com.onionchat.dr0id.ui.broadcast.CreateBroadCastActivity.Companion.EXTRA_BROADCAST_ID
import com.onionchat.dr0id.ui.chat.ChatWindow
import com.onionchat.dr0id.ui.chat.ChatWindow.Companion.EXTRA_PARTNER_ID
import com.onionchat.dr0id.ui.contactdetails.ContactDetailsActivity
import com.onionchat.dr0id.ui.contactdetails.ContactDetailsActivity.Companion.EXTRA_CONTACT_ID
import com.onionchat.dr0id.ui.info.InfoActivtiy
import com.onionchat.dr0id.ui.settings.SettingsActivity
import com.onionchat.dr0id.users.BroadcastManager
import com.onionchat.dr0id.users.UserManager
import com.onionchat.localstorage.userstore.Broadcast
import com.onionchat.localstorage.userstore.Conversation


class ContactListWindow : OnionChatActivity(), ContactListAdapter.ItemClickListener {

    var adapter: ContactListAdapter? = null
    var conversations: ArrayList<Conversation> = ArrayList();

    lateinit var resultLauncher: ActivityResultLauncher<Intent>

    var isAllFabsVisible = false

    val SCAN_RESULT = 101
    val CONTACT_DETAILS_RESULT = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_list)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setOnClickListener {
            UserManager.myId?.let {
                openContactDetails(it, resultLauncher)
            }
        }
        val recyclerView = findViewById<RecyclerView>(R.id.contact_list_id_list)
        recyclerView.setLayoutManager(LinearLayoutManager(this));
        UserManager.getAllUsers().get().forEach {
            conversations.add(Conversation(it, 0))
        }
        BroadcastManager.getAllBroadcasts().get().forEach {
            conversations.add(Conversation(null, 0, broadcast = it))
        }
        adapter = ContactListAdapter(conversations)
        adapter?.setClickListener(this)
        recyclerView.adapter = adapter


        val fab = findViewById<FloatingActionButton>(R.id.contact_list_fab)
        val scanFab = findViewById<FloatingActionButton>(R.id.contact_list_scan_fab)
        val generateFab = findViewById<FloatingActionButton>(R.id.contact_list_generate_fab)
        val broadCastFab = findViewById<FloatingActionButton>(R.id.contact_list_create_broadcast)
        fab.setOnClickListener { view ->
            if (!isAllFabsVisible) {
                scanFab.visibility = View.VISIBLE
                generateFab.visibility = View.VISIBLE
                broadCastFab.visibility = View.VISIBLE
                isAllFabsVisible = true
            } else {
                scanFab.visibility = View.GONE
                generateFab.visibility = View.GONE
                broadCastFab.visibility = View.GONE
                isAllFabsVisible = false
            }
        }

        generateFab.setOnClickListener {
            val intent = Intent(this@ContactListWindow, QrGenerator::class.java)
            intent.putExtra("data", QrPayload.encode(QrPayload(UserManager.myId!!, Crypto.getMyPublicKey()!!.encoded)))
            startActivity(intent)
        }
        scanFab.setOnClickListener {
            val intentIntegrator = IntentIntegrator(this)
            intentIntegrator.setRequestCode(SCAN_RESULT)
            intentIntegrator.setDesiredBarcodeFormats(listOf(IntentIntegrator.QR_CODE))
            intentIntegrator.initiateScan()
        }
        broadCastFab.setOnClickListener {
            val intent = Intent(this@ContactListWindow, CreateBroadCastActivity::class.java)
            startActivity(intent)
        }


        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ContactDetailsActivity.USER_DELETED) {
                val deleted_uid = result.data?.getStringExtra(EXTRA_CONTACT_ID)
                if (deleted_uid != null) {
                    var conversation: Conversation? = null
                    conversations.forEach {
                        if (it.getId().equals(deleted_uid)) {
                            conversation = it
                        }
                    }
                    conversation?.let {
                        conversations.remove(it)
                        adapter?.notifyDataSetChanged()
                    }
                }
            } else if (result.resultCode == CreateBroadCastActivity.BROADCAST_CREATED) {
                val added_id = result.data?.getStringExtra(EXTRA_BROADCAST_ID)
                added_id?.let {
                    val future = BroadcastManager.getBroadcastById(it)
                    future.get()?.let {
                        val pos = conversations.size
                        conversations.add(Conversation(null, 0, true, it))
                        adapter?.notifyItemInserted(pos)
                    }
                }
            }
        }
        //updateConnectionState(ConnectionStatus.CONNECTING)
    }

    override fun onResume() {
        super.onResume()
        deleteNotifications()
        pingAllConversations()
    }

    override fun onConnected(success: Boolean) {
        if (!success) {
            updateConnectionState(ConnectionStatus.ERROR)
        } else {
            runOnUiThread {
                UserManager.myId?.let {
                    setTitle(IDGenerator.toVisibleId(it))
                    //updateConnectionState(ConnectionStatus.CONNECTED)
                    pingAllConversations()
                } ?: kotlin.run {
                    Logging.d("ContactListWindow", "UserManager.myID is null...")
                    updateConnectionState(ConnectionStatus.ERROR)
                }
            }

        }
    }

    override fun onItemClick(view: View?, position: Int) {
        val intent = Intent(this, ChatWindow::class.java)
        conversations[position].unreadMessages = 0
        intent.putExtra(EXTRA_PARTNER_ID, conversations[position].getId())
        startActivity(intent)
    }

    override fun onItemLongClick(view: View?, position: Int) {
        conversations[position].user?.let {
            openContactDetails(it.id, resultLauncher)
        } ?: run {
            conversations[position].broadcast?.let {
                openBroadcastDetails(it.id, resultLauncher)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        try {
            var result = IntentIntegrator.parseActivityResult(resultCode, data)
            if (result != null && result.contents != null) {
                Logging.d("QrScanner", "scanned: <" + result.contents + ">")
                // todo ask user
                conversations.add(Conversation(UserManager.addUser(QrPayload.decode(result.contents))))
                adapter?.notifyDataSetChanged()
            }
        } catch (e: Exception) {
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.contact_list_menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        when (id) {
            R.id.info -> {
                startActivity(Intent(this@ContactListWindow, InfoActivtiy::class.java))
            }
            R.id.new_circuit -> {
                applyNewCirciut()
            }
            R.id.ping_contacts -> {
                pingAllConversations()
            }
            R.id.reconnect -> {
                BackendConnector.getConnector().reconnect(this)
            }
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            else -> {
            }
        }
        return true
    }

    override fun onReceiveMessage(message: Message): Boolean {
        //message.from
        conversations.forEachIndexed { i, it ->
            if (message is IBroadcastMessage) {
                if (it.getId().equals(message.getBroadcastId())) {
                    it.unreadMessages++
                    runOnUiThread {
                        adapter?.notifyItemChanged(i)
                    }
                }
            } else {
                if (it.getLabel().equals(message.from)) {
                    it.unreadMessages++
                    runOnUiThread {
                        adapter?.notifyItemChanged(i)
                    }
                }
            }
        }

        return false
    }

    override fun onPingReceived(user: String) {
        Logging.d("ContactListWindow", "on ping received <" + user + ">")
        conversations.forEach {
            if ((it.getLabel()).equals(user)) {
                if (!it.isOnline) {
                    pingAllConversations()
                } else {
                    Logging.d("ContactListWindow", "User marked as online <" + user + "> no need to do ping")
                }
            }
        }
        // todo warn user if unadded users are going to ping him ?
    }

    fun pingAllConversations() {
        Logging.d("ContactListWindow", "Ping all contacts")
        //updateConnectionState(ConnectionStatus.PINGING)
        Thread {
            conversations.forEachIndexed { i, conversation ->
                if (!conversation.getId().equals(UserManager.myId)) {
                    conversation.user?.let {
                        ConnectionManager.isUserOnline(it) { isOnline ->
                            runOnUiThread {
                                conversation.isOnline = isOnline
                                adapter?.notifyItemChanged(i)
                            }
                        }
                    }
                }
            }
        }.start()
    }

    override fun onBroadcastAdded(broadcast: Broadcast) {
        runOnUiThread {
            val pos = conversations.size
            conversations.add(Conversation(null, 0, true, broadcast))
            adapter?.notifyItemInserted(pos)
        }
    }
}