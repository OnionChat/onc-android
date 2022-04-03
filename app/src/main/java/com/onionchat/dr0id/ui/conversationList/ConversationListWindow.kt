package com.onionchat.dr0id.ui.conversationList

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.zxing.integration.android.IntentIntegrator
import com.onionchat.common.*
import com.onionchat.dr0id.OnionChatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.database.BroadcastManager
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.messages.IBroadcastMessage
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.tasks.CheckConnectionTask
import com.onionchat.dr0id.ui.IAddUserAbleActivity
import com.onionchat.dr0id.ui.IComposeAbleActivity
import com.onionchat.dr0id.ui.OnionChatFragment
import com.onionchat.dr0id.ui.adduser.UserAddConfirmationActivity
import com.onionchat.dr0id.ui.broadcast.CreateBroadCastActivity
import com.onionchat.dr0id.ui.contactdetails.ContactDetailsActivity
import com.onionchat.dr0id.ui.feed.FeedFragment
import com.onionchat.dr0id.ui.feed.FeedViewModel
import com.onionchat.dr0id.ui.info.InfoActivtiy
import com.onionchat.dr0id.ui.settings.SettingsActivity
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.userstore.Broadcast


class ConversationListWindow : OnionChatActivity(), IComposeAbleActivity, IAddUserAbleActivity {

    lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private val viewModel: ConversationListViewModel by viewModels()
    private val feedViewModel: FeedViewModel by viewModels()
    private var viewPager: ViewPager? = null

    val SCAN_RESULT = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.conversation_list_window_activity)


        setupActionBar()
        setupResultLauncher()
        if(savedInstanceState == null) {
            loadConversationsAsync()
        }
        setupViews()
        setupFragments()
    }


    fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setOnClickListener {
            UserManager.myId?.let {
                openContactDetails(it, resultLauncher)
            }
        }
    }

    fun setupResultLauncher() {

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == UserAddConfirmationActivity.RESULT_USER_ADDED) {
                result.data?.getStringExtra(UserAddConfirmationActivity.RESULT_EXTRA_UID)?.let {
                    UserManager.getUserById(it).get()?.let {
                        viewModel.conversations.value?.add(0, Conversation(it))

                        pingAllConversations()
                        return@registerForActivityResult
                    }
                }
                Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_LONG).show()
            } else if (result.resultCode == ContactDetailsActivity.USER_DELETED) {
                val deleted_uid = result.data?.getStringExtra(ContactDetailsActivity.EXTRA_CONTACT_ID)
                if (deleted_uid != null) {
                    var conversation: Conversation? = null
                    viewModel.conversations.value?.forEach {
                        if (it.getId() == deleted_uid) {
                            conversation = it
                        }
                    }
                    conversation?.let {
                        viewModel.conversations.value?.remove(it) // todo this is not supported yet
                    }
                }
            } else if (result.resultCode == CreateBroadCastActivity.BROADCAST_CREATED) {
                val added_id = result.data?.getStringExtra(CreateBroadCastActivity.EXTRA_BROADCAST_ID)
                added_id?.let {
                    val future = BroadcastManager.getBroadcastById(it)
                    future.get()?.let {
                        viewModel.conversations.value?.add(0, Conversation(null, 0, false, it))
                        return@registerForActivityResult
                    }
                    pingAllConversations()
                }
                Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun loadConversationsAsync() {
        Thread {

            var conversations = ArrayList<Conversation>()

            UserManager.getAllUsers().get().forEach { // todo fetch asynchronous
                conversations.add(Conversation(it, 0, lastMessage = MessageManager.getLastMessage(it).get()))
            }
            BroadcastManager.getAllBroadcasts().get().forEach {
                conversations.add(Conversation(null, 0, broadcast = it))
            }
            conversations.sortByDescending { it.lastMessage?.created }
            runOnUiThread {
                viewModel.conversations.value?.addAll(conversations)
            }
        }.start()
    }

    fun setupViews() {
        viewPager = findViewById(R.id.conversation_list_window_viewpager)


    }

    fun setupFragments() {

        viewPager?.adapter = PageAdapter(supportFragmentManager)
//        viewPager?.setPageTransformer(true, CubeOutTransformer())
        viewPager?.currentItem = 0 //savedInstanceState?.getInt(KEY_SELECTED_PAGE) ?: 0
        val tabLayout = findViewById<TabLayout>(R.id.conversation_list_window_viewpager_tablayout)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.getTabAt(0)?.setIcon(R.drawable.outline_contacts_white_24)
        tabLayout.getTabAt(1)?.setIcon(R.drawable.outline_public_white_24)
    }

    inner class PageAdapter internal constructor(fragmentManager: FragmentManager) :
        FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        val fragments = ArrayList<OnionChatFragment>()

        init {
            fragments.add(ConversationListFragment.newInstance())
            fragments.add(FeedFragment.newInstance())
        }

        override fun getItem(position: Int): Fragment =
            fragments[position]

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return fragments[position].getTitle(this@ConversationListWindow)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        try {
            var result = IntentIntegrator.parseActivityResult(resultCode, data)
            if (result != null && result.contents != null) {
                Logging.d("QrScanner", "scanned: <" + result.contents + ">")
                AddUserPayload.decode(result.contents)?.let {
                    openContactAddConfirmation(it, resultLauncher)
                }
            }
        } catch (e: Exception) {
            Logging.e(TAG, "Error while parse activity result", e)
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
                startActivity(Intent(this@ConversationListWindow, InfoActivtiy::class.java))
            }
            R.id.new_circuit -> {
                newTorIdentity()
            }
            R.id.ping_contacts -> {
                pingAllConversations()
            }
            R.id.reconnect -> {
                ConnectionManager.reconnect()
            }
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            else -> {
            }
        }
        return true
    }


    override fun onReceiveMessage(message: IMessage?, encryptedMessage: EncryptedMessage): Boolean {

        var index = -1
        //message.from
        Logging.d(TAG, "onReceiveMessage [+] received message <$message, $encryptedMessage>")
        if (encryptedMessage.hashedTo == Conversation.DEFAULT_FEED_ID) {


            message?.let {
                if (MessageTypes.shouldBeShownInChat(encryptedMessage.type)) {
                    runOnUiThread {
                        Logging.d(TAG, "onReceiveMessage [+] forward to feed")

                        feedViewModel.feed.value?.add(it)
                        Logging.d(TAG, "onReceiveMessage [+] forward to feed done")

                    }
                }
            }
        } else {
            runOnUiThread {
                viewModel.conversations.value?.forEachIndexed { i, it ->
                    if (message is IBroadcastMessage) { // todo fix broadcast
                        if (it.getId().equals(message.getBroadcast().id)) { // todo fix broadcast
                            it.unreadMessages++
                        }
                    } else if (MessageTypes.shouldBeShownInChat(encryptedMessage.type)) {
                        Logging.d(TAG, "onReceiveMessage [+] check for update ${it.getHashedId()} vs ${encryptedMessage.hashedFrom}")

                        if ((it.getHashedId() == encryptedMessage.hashedFrom && UserManager.getMyHashedId() == encryptedMessage.hashedTo) ||
                            (it.getHashedId() == encryptedMessage.hashedTo && it.broadcast != null)
                        ) {
                            Logging.d(TAG, "onReceiveMessage [+] update conversation <${it.getHashedId()}>")
                            index = i
                        }
                    }
                }
                var conversation: Conversation? = null
                if (index >= 0) {
                    runOnUiThread {
                        Logging.d(TAG, "onReceiveMessage [+] update items")
                        conversation = viewModel.conversations.value?.get(index)
                        conversation?.let {
                            Logging.d(TAG, "onReceiveMessage [+] update items of conversation <${conversation}>")

                            it.unreadMessages++
                            it.lastMessage = encryptedMessage
                            if (isTopActivity) {
                                viewModel.conversations.value?.removeAt(index)
                                viewModel.conversations.value?.add(0, it)
                            } else {
                                val action = ListChangeAction.ITEMS_CHANGED
                                action.itemCount = 1
                                action.positionStart = index
                                viewModel.conversationEvents.postValue(action)
                            }
                        }
                    }
                } else {
                    Logging.e(TAG, "onReceiveMessage [-] don't process message <${encryptedMessage.messageId}>")
                }

//            if (index >= 0) {
//                val item = conversations[index]
//                conversations.remove(item)
//                conversations.add(0, item)
//                adapter?.notifyDataSetChanged()
//
//            }
            }
        }

        return false
    }

    fun pingAllConversations() {
        Logging.d("ContactListWindow", "Ping all contacts")
        //updateConnectionState(ConnectionStatus.PINGING)
        val conversations = viewModel.conversations.value?.clone() as List<Conversation>?
        conversations?.forEach { conversation ->
            if (conversation.getId() != UserManager.myId) {
                conversation.user?.let {
                    ConnectionManager.pingUser(it).then { result ->
                        var index = -1

                        runOnUiThread {
                            viewModel.conversations.value?.forEachIndexed { i, conversation ->
                                if (conversation.getHashedId() == it.getHashedId()) {
                                    index = i
                                }
                            }
                            if (index >= 0) {
                                conversation.isOnline = result.status == OnionTask.Status.SUCCESS
                                viewModel.notifyItemRangeChanged(index, 1)
                            }
                        }
                    }
                }
            }
        }
    }

    ///////////////////// service callbacks //////////////7

    // todo rework that shit
    override fun onCheckConnectionFinished(status: CheckConnectionTask.CheckConnectionResult) {
        if (status.status != OnionTask.Status.SUCCESS) {
            updateConnectionState(ConnectionManager.ConnectionState.ERROR)
        } else {
            runOnUiThread {
                UserManager.myId?.let {
                    title = UserManager.getMyLabel(this@ConversationListWindow)
                    //updateConnectionState(ConnectionStatus.CONNECTED)
                    pingAllConversations()
                } ?: kotlin.run {
                    Logging.d("ContactListWindow", "UserManager.myID is null...")
                    updateConnectionState(ConnectionManager.ConnectionState.ERROR)
                }
            }
        }
    }

    override fun onBroadcastAdded(broadcast: Broadcast) {
        runOnUiThread {
            viewModel.conversations.value?.let { conversations ->
                conversations.add(Conversation(null, 0, true, broadcast))
            }
        }
    }

    companion object {
        val TAG = "ConversationListWindow"

    }

    override fun getActivityResultLauncher(): ActivityResultLauncher<Intent> {
        return resultLauncher
    }

    override fun getUserScanRequestCode(): Int {
        return SCAN_RESULT
    }
}