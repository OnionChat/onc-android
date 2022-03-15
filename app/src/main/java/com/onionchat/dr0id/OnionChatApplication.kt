package com.onionchat.dr0id

import android.app.Application
import com.onionchat.connector.WebHelper
import com.onionchat.dr0id.database.DatabaseManager
import com.onionchat.dr0id.queue.OnionTaskProcessor

class OnionChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WebHelper.extractDemo(this)
        DatabaseManager.initDatabase(this) // todo move to content provider ?
        OnionTaskProcessor.attachContext(this) // todo move to content provider ?
    }
}