package com.onionchat.dr0id.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.legacy.content.WakefulBroadcastReceiver
import com.onionchat.common.Logging
import com.onionchat.dr0id.broadcast.AlarmReceiver

object ServicesManager {

    const val TAG = "ServicesManager"

    fun launchServices(context: Context) {
        Logging.d(TAG, "launchServices [+] going to launch services")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AlarmReceiver.scheduleAlarm(context)
            WakefulBroadcastReceiver.startWakefulService(context, Intent(context, OnionChatSyncService::class.java))

//            context?.startForegroundService()
        } else {
            context?.startService(Intent(context, OnionChatSyncService::class.java))
        }
        context?.startService(Intent(context, OnionChatConnectionService::class.java))
    }
}