package com.onionchat.dr0id.ui.importer

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.databinding.FragmentBroadcastReceiversBinding
import com.onionchat.dr0id.ui.contactlist.ContactListAdapter
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.databinding.FragmentSelectRecipientsBinding
import com.onionchat.localstorage.userstore.User

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SelectRecipientsFragment : Fragment(), ContactListAdapter.ItemClickListener {

    private var _binding: FragmentSelectRecipientsBinding? = null
    var adapter: ContactListAdapter? = null
    var conversations: ArrayList<Conversation> = ArrayList();
    //var listener: IBroadcastCreateListener? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSelectRecipientsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = binding.fragmentRecipientsSelectionView
        recyclerView.setLayoutManager(LinearLayoutManager(context));
        UserManager.getAllUsers().get().forEach {
            conversations.add(Conversation(it, 0))
        }
        val feedConversation = Conversation(user = null, feedId = Conversation.DEFAULT_FEED_ID)

        feedConversation.cachedBitmap = BitmapFactory.decodeResource(requireContext().getResources(),
            R.drawable.outline_public_white_48);
        conversations.add(0, feedConversation)
        adapter = ContactListAdapter(conversations, true)
        adapter?.setClickListener(this)
        recyclerView.adapter = adapter

//        binding.fragmentBroadcastReceiversCreate.setOnClickListener {
////            findNavController().navigate(R.id.action_Second2Fragment_to_First2Fragment) // todo store broadcast
//            var users_to_add = mutableListOf<User>()
//            conversations.forEach {
//                if (it.selected) {
//                    it.user?.let {
//                        users_to_add.add(it)
//                    }
//                }
//            }
//            listener?.onUsersSelected(users_to_add)
//            listener?.onCreateBroadcast()
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
//        if (activity is IBroadcastCreateListener) {
//            listener = activity
//        }
    }

    override fun onItemClick(view: View?, position: Int) {

    }

    override fun onItemLongClick(view: View?, position: Int) {
    }

    override fun onCheckedChangeListener(position: Int) {

    }
}