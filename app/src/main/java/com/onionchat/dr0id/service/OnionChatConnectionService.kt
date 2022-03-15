package com.onionchat.dr0id.service

import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.onionchat.common.Logging
import com.onionchat.common.SettingsManager
import com.onionchat.connector.BackendConnector
import com.onionchat.connector.IConnectorCallback
import com.onionchat.connector.http.OnionServer
import com.onionchat.dr0id.MainActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.database.BroadcastManager
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.dr0id.messaging.messages.BroadcastTextMessage
import com.onionchat.dr0id.messaging.messages.IBroadcastMessage
import com.onionchat.dr0id.messaging.messages.ITextMessage
import com.onionchat.dr0id.queue.OnionFuture
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.tasks.ProcessPendingTask
import com.onionchat.dr0id.queue.tasks.ReceiveMessageTask
import com.onionchat.dr0id.queue.tasks.SendMessageTask
import com.onionchat.localstorage.userstore.Broadcast
import com.onionchat.localstorage.userstore.User
import java.lang.System.exit
import java.util.*
import kotlin.collections.ArrayList


class OnionChatConnectionService : Service(), OnionTaskProcessor.OnionTaskProcessorObserver {

    val TAG = "OnionChatConnectionService"

    val ACTION_KILL_SERVICE = "kill_onionchat_service"

    val NOTIFICATION_CHANNEL_MESSAGES = "notification_messages"

    interface ServiceClient {
        fun onReceiveMessage(message: IMessage): Boolean
        fun onPingReceived(user: String)
        fun onBroadcastAdded(broadcast: Broadcast)
    }

    val cachedMessages = Collections.synchronizedList(ArrayList<IMessage>())
    var serviceClients = Collections.synchronizedSet(HashSet<ServiceClient>())


    private val binder = LocalBinder()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        // If we get killed, after returning from here, restart
        return START_STICKY
    }


    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): OnionChatConnectionService = this@OnionChatConnectionService
    }


    override fun onBind(intent: Intent): IBinder {
        Logging.d("OnionChatConnectionService", "onBind()")
        return binder
    }


    override fun onCreate() {
        startForeground(101, buildForegroundNotification())

        OnionTaskProcessor.addObserver(this)

        Logging.d(TAG, "Applying wakelock")
        val wakeLock: PowerManager.WakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run { // todo make optional
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OnionChat::keepListeningForMessages").apply {
                    acquire() // todo add timeout !?
                }
            }

        BackendConnector.registerOnReceiveCallback { type, data ->
            when (type) {
                OnionServer.ReceiveDataType.PING -> {
                    OnionTaskProcessor.enqueue(ProcessPendingTask())
                    if (serviceClients.isEmpty()) {
                        if (SettingsManager.getBooleanSetting(getString(R.string.key_ping_notifications), this)) {
                            showNotification("You were pinged", "You have been pinged by a user. May you wanna chat?", "ping_channel")
                        }
                    } else {
                        serviceClients.forEach {
                            it.onPingReceived(data)
                        }
                    }
                }
                OnionServer.ReceiveDataType.RESPONSEPUB -> {

                }
                OnionServer.ReceiveDataType.REQUESTPUB -> {

                }
                OnionServer.ReceiveDataType.SYMKEY -> {

                }
                OnionServer.ReceiveDataType.POSTMESSAGE -> {
                    OnionTaskProcessor.enqueue(ReceiveMessageTask(data)).then { result ->
                        if (result.status == OnionTask.Status.SUCCESS) {
                            result.message?.let {
                                onReceiveMessage(it)
                            } ?: run {
                                Logging.e(TAG, "oncreate [-] ReceiveMessageTask returned invalid result $result")
                            }
                        } else {
                            // todo how to handle this?
                        }
                    }
                }
                else -> {
                    Logging.e(TAG, "Unsupported message type <$type>")
                }
            }
        }

        ConnectionManager.checkConnection(this@OnionChatConnectionService) {
            Logging.d("OnionChatConnectionService", "service is connected")
            UserManager.myId = BackendConnector.getConnector().getHostName(this@OnionChatConnectionService)
            OnionTaskProcessor.enqueue(ProcessPendingTask()) // todo make more nice !
        }
        val br: BroadcastReceiver = MyBroadCastReceiver()
        val filter = IntentFilter(ACTION_KILL_SERVICE)
        registerReceiver(br, filter);
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

    private fun buildForegroundNotification(): Notification {


        val morePendingIntent = PendingIntent.getBroadcast(
            this.applicationContext,
            1,
            Intent(ACTION_KILL_SERVICE),
            FLAG_IMMUTABLE
        )

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("service_channel", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val b: NotificationCompat.Builder = NotificationCompat.Builder(this, channelId)
        b.setOngoing(true)
            .setContentTitle(getString(R.string.connected))
            .setContentText("OnionChat is connected in background and listening for messages")
            .setSmallIcon(R.drawable.onion_pixel_green)
            .setTicker(getString(R.string.connected))
            .addAction(
                R.drawable.onion_pixel_green, getString(R.string.exit_service),
                morePendingIntent
            );
        return b.build()
    }

    inner class MyBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Logging.d("OnionChatConnectionService", "kill foreground service")
            if (intent.getAction().equals(ACTION_KILL_SERVICE)) {
                this@OnionChatConnectionService.stopForeground(true)
                stopSelf()
                Handler(Looper.getMainLooper()).post {
                    exit(0)
                }
            }
        }
    }

    fun checkConnection(callback: IConnectorCallback) {
        Logging.d("OnionChatConnectionService", "checkConnection()")
        ConnectionManager.checkConnection(this@OnionChatConnectionService, callback)
    }

    fun addClient(callback: ServiceClient) {
        serviceClients.add(callback)
        if (cachedMessages.size > 0) {
            serviceClients.forEach { listener ->
                cachedMessages.forEach { message ->
                    listener.onReceiveMessage(message)
                }
            }
            cachedMessages.clear()
        }
    }

    fun removeClient(callback: ServiceClient) {
        serviceClients.remove(callback) // TODO add thread savety
    }

    private fun showNotification(title: String, message: String, channel: String) {
        val noificationId = Random().nextInt(100)
        val channelId = channel
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0, intent, FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(
            applicationContext, channelId
        )
        builder.setSmallIcon(R.drawable.onion_pixel_green)
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        builder.setContentTitle(title) // make suer change the channel for image
        builder.setContentText(message) //notification for image
        // builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_MAX
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                val notificationChannel = NotificationChannel(
                    channelId, "OnionChat Channel",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.description = "This notification channel is used to notify user"
                notificationChannel.enableVibration(true)
                notificationChannel.enableLights(true)
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
        val notification = builder.build()
        notificationManager?.notify(noificationId, notification)
    }

    fun onBroadcastAdded(broadcast: Broadcast) {
        serviceClients.forEach {
            it.onBroadcastAdded(broadcast)
        }
    }

    val forwardedSignatures = HashSet<String>()

//    fun forwardBroadcastMessage(broadcast: Broadcast?, message: BroadcastTextMessage) {
//        if (forwardedSignatures.contains(message.signature)) {
//            return
//        } //
//        broadcast?.let {
//            Thread {
//                // do that asynchronous
//                BroadcastManager.getBroadcastUsers(it).get()?.forEach { // todo send this only to users added to the broadcast
//                    if (it.getName() != message.from) {
//                        val encoded_message = MessageProcessor.encodeMessage(message, it.certId)
//                        if (!forwardedSignatures.contains(message.signature)) {
//                            Communicator.sendMessage(it.id, encoded_message) { status ->
//                                if (status == Communicator.MessageSentStatus.SENT) {
//                                    Logging.d("OnionChatConnectionService", "Successfully forwarded message to user <" + it.getName() + ">")
//                                }
//                            }
//                            forwardedSignatures.add(message.signature)
//                        }
//                    }
//                }
//            }.start()
//        }
//        forwardedSignatures.add(message.signature)
//    }

    fun onReceiveMessage(message: IMessage): Boolean {
        Logging.d("OnionChatConnectionService", "onReceiveMessage [+] message <" + message + ">")
        // broadcast check for broadcasts to be added
        if (message is BroadcastTextMessage) { // todo will this still work ?
            var broadcast = BroadcastManager.getBroadcastById(message.getBroadcast().id).get()
            if (broadcast == null) {
                val allowed = SettingsManager.getBooleanSetting(getString(R.string.key_allow_broadcast_adding), this)
                if (allowed) {
                    broadcast = Broadcast(message.getBroadcast().id, message.getBroadcast().label)
                    BroadcastManager.addBroadcast(broadcast)
                    val default_add_all_users = SettingsManager.getBooleanSetting(getString(R.string.key_default_add_all_users), this)
                    if (default_add_all_users) {
                        BroadcastManager.addUsersToBroadcast(broadcast, UserManager.getAllUsers().get())
                    }
                    onBroadcastAdded(broadcast)
                }
                Logging.d("OnionChatConnectionService", "onReceiveMessage [+] broadcast auto add disabled")
            }
//            if (doForward) {
//                message.getEncryptedMessage()?.let {
//                    OnionTaskProcessor.enqueue(ForwardMessageTask(it))
//                } ?: kotlin.run {
//                    Logging.e(TAG, "onReceiveMeessage [-] unable to forward broadcast message")
//                }
//            } else {
//                Logging.d("OnionChatConnectionService", "onReceiveMessage [+] message forwarding disabled")
//            }
        }


        // todo add check if listeners are registered !?
        Logging.d("OnionChatConnectionService", "onReceiveMessage [+] serviceClients <" + serviceClients + ">")
        var consumed = false
        serviceClients.forEach {
            if (it.onReceiveMessage(message)) {
                consumed = true
            }
        }
        if (!consumed) {
            if (SettingsManager.getBooleanSetting(getString(R.string.key_notifications), this)) {
                var tag = "New Messages"
                var text = "You have received new messages. Click to show them up"
                if (message is SymmetricMessage) {
                    tag = message.hashedFrom
                }
                if (message is IBroadcastMessage) {
                    tag += "@${message.getBroadcast().label}"
                }
                if (message is ITextMessage) {
                    text = message.getText().text
                }
                showNotification(tag, text, NOTIFICATION_CHANNEL_MESSAGES)
            }
            cachedMessages.add(message)
        }
        return true
    }

    fun deleteNotifications() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_MESSAGES);
    }

    fun sendMessage(
        message: IMessage,
        fromUID: String,
        to: User
    ): OnionFuture<SendMessageTask.SendMessageResult> {
        return OnionTaskProcessor.enqueue(SendMessageTask(message, fromUID, to))
    }

    override fun onTaskEnqueued(task: Any) {
    }

    override fun onTaskFinished(task: Any, result: OnionTask.Result) {
    }


}