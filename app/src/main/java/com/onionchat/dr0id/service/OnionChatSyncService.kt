package com.onionchat.dr0id.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.dr0id.broadcast.AlarmReceiver
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.connectivity.ConnectionStateChangeListener
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.tasks.ProcessPendingTask
import java.util.*


class OnionChatSyncService : Service(), ConnectionStateChangeListener {

    var wakeLock: PowerManager.WakeLock? = null

    val start = System.currentTimeMillis()

    fun aquireWakelock(timeout: Long) {
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run { // todo make optional
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OnionChat::keepListeningForMessages").apply {
                    acquire(timeout) // todo add timeout !?
                }
            }
    }

    override fun onCreate() {
        super.onCreate()
        Logging.d(TAG, "starting $start")

        startForeground(102, buildForegroundNotification())
        val keepAliveMinutes = resources.getInteger(R.integer.poll_keep_alive_minutes)
        aquireWakelock(keepAliveMinutes * 60 * 1000L)

        UserManager.myId = ConnectionManager.getHostName(this@OnionChatSyncService)

        ConnectionManager.registerConnectionStateChangeListener(this)

        ConnectionManager.checkConnection().then {
            if (it.status == OnionTask.Status.SUCCESS) {
                Logging.d(TAG, "service is connected")
                onConnected()

            } else {
                Logging.d(TAG, "onCreate [-] service is not connected")
                finishService()
            }
        }
    }

    fun onConnected() {
        Logging.d(TAG, "onCreate [+]  enqueue pending task")
        OnionTaskProcessor.enqueue(ProcessPendingTask()).then {
            Logging.d(TAG, "onCreate [+] pending task finished")
            finishService()
        }
    }


    private fun buildForegroundNotification(): Notification {


//        val morePendingIntent = PendingIntent.getBroadcast(
//            this.applicationContext,
//            1,
//            Intent(ACTION_KILL_SERVICE),
//            PendingIntent.FLAG_IMMUTABLE
//        )

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("refresh_service_channel", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val b: NotificationCompat.Builder = NotificationCompat.Builder(this, channelId)
        b.setOngoing(true)
            .setContentTitle(getString(R.string.sync_title))
            .setContentText(getString(R.string.sync_message))
            .setSmallIcon(R.drawable.onion_pixel_green)
            .setTicker(getString(R.string.connected))
//            .addAction(
//                R.drawable.onion_pixel_green, getString(R.string.exit_service),
//                morePendingIntent
//            );
        return b.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
    var intent:Intent? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.intent = intent
        return super.onStartCommand(intent, flags, startId)
    }

    fun finishService() {
        val keepAliveMinutes = resources.getInteger(R.integer.poll_keep_alive_minutes)
        Logging.d(TAG, "finishService [+] keep service running for $keepAliveMinutes minutes to receive messages")
        Thread {
            val diff = System.currentTimeMillis() - start
            val sleeptime = (keepAliveMinutes * 60 * 1000L) - diff
            Logging.d(TAG, "finishService [-] service was running $diff millis.. going to keep service running <$sleeptime> millis")
            if (sleeptime > 0) {
                SystemClock.sleep(sleeptime)
            }
            Logging.d(TAG, "finishService [+] background task has done it's work... lets kill him")
            stopSelf()
            stopForeground(true)
            wakeLock?.release()
            intent?.let {
                AlarmReceiver.completeWork(it)
            }

        }.start()
    }

    companion object {
        const val TAG = "OnionChatSyncService"
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onConnectionStateChanged(state: ConnectionManager.ConnectionState) {
        if(state == ConnectionManager.ConnectionState.CONNECTED) {
            onConnected()
        }
    }
}