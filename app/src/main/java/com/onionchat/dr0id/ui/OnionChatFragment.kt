package com.onionchat.dr0id.ui

import android.content.Context
import androidx.fragment.app.Fragment

abstract class OnionChatFragment : Fragment() {
    abstract fun getTitle(context: Context) : String
}