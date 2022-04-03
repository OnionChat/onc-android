package com.onionchat.dr0id.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.onionchat.common.Logging
import com.onionchat.dr0id.service.ServicesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        Logging.d(TAG, "onReceive [+] received boot")
        if (p0 == null) {
            Logging.e(TAG, "onRecieve [-] context is null. Abort.")
            return
        }
        ServicesManager.launchServices(p0)
    }

    companion object {
        const val TAG = "BootReceiver"

    }
}