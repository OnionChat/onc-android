package com.onionchat.dr0id

import android.app.Application
import com.onionchat.connector.WebHelper
import com.onionchat.dr0id.users.DatabaseManager
import com.onionchat.dr0id.users.UserManager

class OnionChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WebHelper.extractDemo(this)
        DatabaseManager.initDatabase(this)
    }
}