package com.onionchat.dr0id.ui.broadcast

import com.onionchat.localstorage.userstore.User

interface IBroadcastCreateListener {
    fun onLabelChoosen(label: String)
    fun onUsersSelected(users: List<User>)
    fun onCreateBroadcast()
}