package com.onionchat.dr0id.ui.broadcast

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onionchat.dr0id.R
import com.onionchat.dr0id.databinding.FragmentBroadcastReceiversBinding
import com.onionchat.dr0id.ui.contactlist.ContactListAdapter
import com.onionchat.dr0id.users.UserManager
import com.onionchat.localstorage.userstore.Conversation

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SelectBroadCastReceiversFragment : Fragment() {

    private var _binding: FragmentBroadcastReceiversBinding? = null
    var adapter: ContactListAdapter? = null
    var conversations: ArrayList<Conversation> = ArrayList();

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentBroadcastReceiversBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = binding.fragmentBroadcastReceiversSelection
        recyclerView.setLayoutManager(LinearLayoutManager(context));
        UserManager.getAllUsers().get().forEach {
            conversations.add(Conversation(it, 0))
        }
        adapter = ContactListAdapter(conversations)
        //adapter?.setClickListener(this)
        recyclerView.adapter = adapter

        binding.fragmentBroadcastReceiversCreate.setOnClickListener {
//            findNavController().navigate(R.id.action_Second2Fragment_to_First2Fragment) // todo store broadcast
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}