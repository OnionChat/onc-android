package com.onionchat.dr0id.ui.conversationList

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import com.onionchat.common.AddUserPayload
import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.ui.IAddUserAbleActivity
import com.onionchat.dr0id.ui.OnionChatFragment
import com.onionchat.dr0id.ui.adduser.QrViewer
import com.onionchat.dr0id.ui.broadcast.CreateBroadCastActivity
import com.onionchat.dr0id.ui.chat.ChatWindow
import com.onionchat.dr0id.ui.contactlist.ContactListAdapter
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer.showError

class ConversationListFragment : OnionChatFragment(), ContactListAdapter.ItemClickListener {

    companion object {
        fun newInstance() = ConversationListFragment()
        const val TAG = "ConversationListFragment"
    }

    private val viewModel: ConversationListViewModel by activityViewModels()
    private var conversationListAdapter: ContactListAdapter? = null
    private var recyclerView: RecyclerView? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var isAllFabsVisible = false

    override fun getTitle(context: Context) : String {
        return context.getString(R.string.conversation_list_fragment_title)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.conversation_list_fragment, container, false)
        setupViews(view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupObservers()
    }

    fun setupViews(rootView: View) {
        if (context == null) {
            Logging.e(TAG, "setupViews [-] context is null... abort")
            return
        }
        recyclerView = rootView.findViewById(R.id.conversation_list_fragment_recycler_view)
        linearLayoutManager = LinearLayoutManager(requireContext())
        recyclerView?.layoutManager = linearLayoutManager
        viewModel.conversations.value?.let {
            conversationListAdapter = ContactListAdapter(it)
        } ?: kotlin.run {
            showError(requireContext(), "Error while initialize $TAG", ErrorViewer.ErrorCode.CONVERSATION_LIST_FRAGMENT_CONVERSATIONS_NULL)
        }
        conversationListAdapter?.setClickListener(this)
        recyclerView?.adapter = conversationListAdapter
        val fab = rootView.findViewById<FloatingActionButton>(R.id.contact_list_fab)
        val scanFab = rootView.findViewById<FloatingActionButton>(R.id.contact_list_scan_fab)
        val generateFab = rootView.findViewById<FloatingActionButton>(R.id.contact_list_generate_fab)
        val broadCastFab = rootView.findViewById<FloatingActionButton>(R.id.contact_list_create_broadcast)



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
            val intent = Intent(requireContext(), QrViewer::class.java)
            UserManager.getMyLabel(requireContext())?.let {
                intent.putExtra("data", AddUserPayload.encode(AddUserPayload(UserManager.myId!!, Crypto.getMyPublicKey()!!.encoded, it)))
                startActivity(intent) // todo move to activity launcher
            } ?: run {
                Toast.makeText(requireContext(), getString(R.string.unknown_error), Toast.LENGTH_LONG).show()
            }
        }
        scanFab.setOnClickListener {
            val activity = activity
            if(activity is IAddUserAbleActivity) {
                val intentIntegrator = IntentIntegrator(activity)
                intentIntegrator.setRequestCode(activity.getUserScanRequestCode())
                intentIntegrator.setDesiredBarcodeFormats(listOf(IntentIntegrator.QR_CODE))
                intentIntegrator.initiateScan() // todo move to activity launcher
            }
        }
        broadCastFab.setOnClickListener {
            val activity = activity
            if(activity is IAddUserAbleActivity) {
                val intent = Intent(requireContext(), CreateBroadCastActivity::class.java)
                activity.getActivityResultLauncher().launch(intent) // todo move to activity launcher
            }
        }
    }

    fun setupObservers() {
        viewModel.conversationEvents.observe(viewLifecycleOwner) { listChangeAction ->
            Logging.d(TAG, "listChangeAction [+] got action <$listChangeAction>")

            when (listChangeAction) {
                ListChangeAction.ITEMS_INSERTED -> {

                    val pos = linearLayoutManager?.findFirstCompletelyVisibleItemPosition()
                    if (pos != null && pos <= 0) {
                        recyclerView?.smoothScrollToPosition(0)
                    }
                    conversationListAdapter?.notifyItemRangeInserted(listChangeAction.positionStart, listChangeAction.itemCount)

                }
                ListChangeAction.ITEMS_CHANGED -> {
                    conversationListAdapter?.notifyItemRangeChanged(listChangeAction.positionStart, listChangeAction.itemCount)
                }
                ListChangeAction.ITEMS_DELETED -> {
                    conversationListAdapter?.notifyItemRangeRemoved(listChangeAction.positionStart, listChangeAction.itemCount)
                }
                else -> {
                    Logging.e(TAG, "onActivityCreated [-] unknown ListChangeAction ${listChangeAction}")
                }
            }
        }
        viewModel.conversations.observe(viewLifecycleOwner) {
//            imageView.visibility = View.VISIBLE
//            progress.visibility = View.GONE
//            it.into(imageView)
        }
    }


    override fun onItemClick(view: View?, position: Int) {
        val conversations = viewModel.conversations.value
        if (conversations == null) {
            showError(requireContext(), "Error while open Chat", ErrorViewer.ErrorCode.CONVERSATION_LIST_FRAGMENT_CONVERSATIONS_NULL)
            return
        }
        val intent = Intent(requireContext(), ChatWindow::class.java)
        if (position < 0 || position >= conversations.size) {
            return
        }
        conversations[position].unreadMessages = 0
        val action = ListChangeAction.ITEMS_CHANGED
        action.itemCount = 1
        action.positionStart = position
        viewModel.conversationEvents?.postValue(action)
        intent.putExtra(ChatWindow.EXTRA_PARTNER_ID, conversations[position].getId())
        startActivity(intent)
    }

    override fun onItemLongClick(view: View?, position: Int) {
        val conversations = viewModel.conversations.value
        if (conversations == null) {
            showError(requireContext(), "Error while open Chat", ErrorViewer.ErrorCode.CONVERSATION_LIST_FRAGMENT_CONVERSATIONS_NULL)
            return
        } // todo implement
//        conversations[position].user?.let {
//            openContactDetails(it.id, resultLauncher)
//        } ?: run {
//            conversations[position].broadcast?.let {
//                openBroadcastDetails(it.id, resultLauncher)
//            }
//        }
    }

    override fun onCheckedChangeListener(position: Int) {
    }

}