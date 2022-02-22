package com.onionchat.dr0id.ui.broadcast

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.onionchat.dr0id.databinding.FragmentBroadcastNameBinding
import com.onionchat.dr0id.users.BroadcastManager

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SelectBroadCastNameFragment : Fragment() {

    private val regexPattern = "^(?=.{5,20}$)(?!.*[._-]{2})[a-z][a-z0-9._-]*[a-z0-9]$"

    private var _binding: FragmentBroadcastNameBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentBroadcastNameBinding.inflate(inflater, container, false)
        binding.fragmentBroadcastNameEnterName.addTextChangedListener {
            val str = it.toString()
            if (!str.matches(Regex(regexPattern))) {
                binding.fragmentBroadcastNameEnterName.error = "Doesn't match requirements"
            } else {
                binding.fragmentBroadcastNameEnterName.error = null
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentBroadcastNameNext.setOnClickListener {
            //findNavController().navigate(R.id.action_selectname_to_selectreceivers) // todo switch to contact selection
            val label = binding.fragmentBroadcastNameEnterName.text.toString()
//            val test = arrayOf(
//                "abc",
//                "abcde",
//                "acd_e",
//                "_abcd",
//                "abcd_",
//                "a__bc",
//                "a_.bc",
//                "a_b.c-d",
//                "a_b_c_d_e",
//                "this-is-too-long"
//            )
//            for (s in test) {
//                System.out.format(
//                    "%s %B %n", s,
//                )
//            }
            if (label.matches(Regex(regexPattern))) {
                val broadcast = BroadcastManager.createBroadcast(label).get()
                broadcast?.let {
                    val i = Intent()
                    i.putExtra(CreateBroadCastActivity.EXTRA_BROADCAST_ID, broadcast.id)
                    activity?.setResult(CreateBroadCastActivity.BROADCAST_CREATED, i)
                    activity?.finish()
                }

            } else {
                binding.fragmentBroadcastNameEnterName.error = "Doesn't match requirements"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}