package com.onionchat.dr0id.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.onionchat.common.Crypto
import com.onionchat.common.SettingsManager
import com.onionchat.dr0id.MainActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.databinding.FragmentThirdBinding
import com.onionchat.dr0id.databinding.UserLabelFragmentBinding
import com.onionchat.localstorage.EncryptedLocalStorage

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class UserLabelFragment : Fragment() {

    private val regexPattern = "^(?=.{5,20}$)(?!.*[._-]{2})[a-z][a-z0-9._-]*[a-z0-9]$"


    private var _binding: UserLabelFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = UserLabelFragmentBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.userLabelFragmentButton.setOnClickListener {
            context?.let {
                binding.userLabelFragmentEdit.text?.toString()?.let { label ->
                    if (!label.matches(Regex(regexPattern))) {
                        binding.userLabelFragmentEdit.error = "Doesn't match requirements"
                    } else {
                        binding.userLabelFragmentEdit.error = null
                        Crypto.getMyPublicKey()?.let { cert ->
                            context?.let {
                                EncryptedLocalStorage(cert, Crypto.getMyKey(), it).apply {
                                    if(!storeValue(getString(R.string.key_user_label), label)) {
                                        binding.userLabelFragmentEdit.error = "Unable to store value"
                                    } else {
                                        SettingsManager.setBooleanSetting(getString(R.string.key_onboarding), false, it)
                                        startActivity(Intent(it, MainActivity::class.java))
                                    }
                                }
                            } ?: {
                                binding.userLabelFragmentEdit.error = "Unexpected error"
                            }
                        }?: run{
                            binding.userLabelFragmentEdit.error = "No crypto available"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}