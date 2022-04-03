package com.onionchat.dr0id.service

import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.common.SettingsManager
import com.onionchat.connector.OnReceiveClientDataListener
import com.onionchat.connector.http.OnionServer
import com.onionchat.dr0id.MainActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.connectivity.ConnectionManager.registerConnectionStateChangeListener
import com.onionchat.dr0id.connectivity.ConnectionStateChangeListener
import com.onionchat.dr0id.connectivity.PingPayload
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.dr0id.messaging.messages.IBroadcastMessage
import com.onionchat.dr0id.messaging.messages.ITextMessage
import com.onionchat.dr0id.queue.OnionFuture
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.tasks.*
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.userstore.Broadcast
import com.onionchat.localstorage.userstore.PingInfo
import java.io.InputStream
import java.lang.System.exit
import java.util.*
import kotlin.collections.ArrayList


class OnionChatConnectionService : Service(), OnionTaskProcessor.OnionTaskProcessorObserver, ConnectionStateChangeListener {

    val TAG = "OnionChatConnectionService"

    val ACTION_KILL_SERVICE = "kill_onionchat_service"

    val NOTIFICATION_CHANNEL_MESSAGES = "notification_messages"

    interface ServiceClient {
        fun onReceiveMessage(message: IMessage?, encryptedMessage: EncryptedMessage): Boolean
        fun onPingReceived(payload: PingPayload): Boolean
        fun onBroadcastAdded(broadcast: Broadcast)
        fun onStreamRequested(inputStream: InputStream): Boolean
        fun onConnectionStateChanged(state: ConnectionManager.ConnectionState)
    }

    var serviceClients = Collections.synchronizedList(ArrayList<ServiceClient>()) // todo WeakReference !?


    private val binder = LocalBinder()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        // If we get killed, after returning from here, restart
        return START_NOT_STICKY//START_STICKY
    }


    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): OnionChatConnectionService = this@OnionChatConnectionService
    }


    override fun onBind(intent: Intent): IBinder {
        Logging.d(TAG, "onBind()")
        return binder
    }


    override fun onCreate() {
//

        OnionTaskProcessor.addObserver(this)
        if (!SettingsManager.getBooleanSetting(getString(R.string.key_enable_powersafe), this)) {
            Logging.d(ProcessPendingTask.TAG, "run [+[ powersafe mode is disabled")
            Logging.d(TAG, "Applying wakelock")
            val wakeLock: PowerManager.WakeLock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager).run { // todo make optional
                    newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OnionChat::keepListeningForMessages").apply {
                        acquire() // todo add timeout !?
                    }
                }
            startForeground(101, buildForegroundNotification())
        }


        ConnectionManager.registerOnReceiveCallback(object : OnReceiveClientDataListener {
            override fun onDownloadApk() {
            }

            override fun onReceive(type: OnionServer.ReceiveDataType, data: String) {
                when (type) {
                    OnionServer.ReceiveDataType.PING -> {
                        OnionTaskProcessor.enqueuePriority(HandlePingTask(data)).then {
                            val pingData = it.payload
                            if (serviceClients.isEmpty()) {
                                if (SettingsManager.getBooleanSetting(getString(R.string.key_ping_notifications), this@OnionChatConnectionService)) {
                                    showNotification(getString(R.string.ping_notification_title), getString(R.string.ping_notification_message), "ping_channel")
                                }
                            } else {
                                serviceClients.reversed().forEach { client ->
                                    pingData?.let {
                                        if (client.onPingReceived(it)) { // todo extract ping data ?
                                            return@then
                                        }
                                    }
                                }
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
                        Logging.d(TAG, "oncreate [+] onReceive [+] starting ReceiveMessageTask")

                        OnionTaskProcessor.enqueuePriority(ReceiveMessageTask(data)).then { result ->
                            if (result.status == OnionTask.Status.SUCCESS) {
                                if (result.encryptedMessage == null) {
                                    Logging.e(TAG, "oncreate [-] ReceiveMessageTask returned invalid result $result")
                                } else {
                                    onReceiveMessage(result.message, result.encryptedMessage)
                                }
                            } else {
                                // todo how to handle this?
                            }
                            if (result.broadcast != null) { // todo check status
                                onBroadcastAdded(result.broadcast)
                            }
                        }
                    }
                    else -> {
                        Logging.e(TAG, "Unsupported message type <$type>")
                    }
                }
            }

            override fun onStreamRequested(inputStream: InputStream): Boolean {
                var consumed = false
                serviceClients.forEach {
                    if (it.onStreamRequested(inputStream)) {
                        consumed = true
                    }
                }
                return consumed
            }

        })
        UserManager.myId = ConnectionManager.getHostName(this)
        registerConnectionStateChangeListener(this)
        ConnectionManager.checkConnection().then {
            Logging.d(TAG, "service is connected")
//            UserManager.myId = BackendConnector.CONNECTOR?.getHostName(this@OnionChatConnectionService)
            //OnionTaskProcessor.enqueue(ProcessPendingTask()) // todo make more nice !
        }
        val br: BroadcastReceiver = MyBroadCastReceiver()
        val filter = IntentFilter(ACTION_KILL_SERVICE)
        registerReceiver(br, filter);

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                it.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        //take action when network connection is gained
                        Logging.d(TAG, "onAvailable [+] network status changed <$network>")
                        onNetworkStatusChanged()
                    }

                    override fun onLosing(network: Network, maxMsToLive: Int) {
                        super.onLosing(network, maxMsToLive)
                        Logging.d(TAG, "onLosing [+] network status changed <$network>")
                        onNetworkStatusChanged()
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        Logging.d(TAG, "onLost [+] network status changed <$network>")
                        onNetworkStatusChanged()
                    }

                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                        super.onCapabilitiesChanged(network, networkCapabilities)
                        Logging.d(TAG, "onCapabilitiesChanged [+] network status changed <$network>")
                        onNetworkStatusChanged()
                    }

                    override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                        super.onLinkPropertiesChanged(network, linkProperties)
                        Logging.d(TAG, "onLinkPropertiesChanged [+] network status changed <$network>")
                        onNetworkStatusChanged()
                    }

                    override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                        super.onBlockedStatusChanged(network, blocked)
                        Logging.d(TAG, "onBlockedStatusChanged [+] network status changed <$network>")
                        onNetworkStatusChanged()
                    }
                })
            } else {
                // todo fix android M ?
            }
        }
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
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.onion_chat_service_foregreound_message))
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
            Logging.d(TAG, "kill foreground service")
            if (intent.getAction().equals(ACTION_KILL_SERVICE)) {
                this@OnionChatConnectionService.stopForeground(true)
                stopSelf()
                Handler(Looper.getMainLooper()).post {
                    exit(0)
                }
            }
        }
    }

    fun checkConnection(): OnionFuture<CheckConnectionTask.CheckConnectionResult> {
        Logging.d(TAG, "checkConnection()")
        return ConnectionManager.checkConnection()
    }

    fun addClient(callback: ServiceClient) {
        if (!serviceClients.contains(callback)) {
            serviceClients.add(0, callback)
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
//                                    Logging.d(TAG, "Successfully forwarded message to user <" + it.getName() + ">")
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

    fun onReceiveMessage(message: IMessage?, encryptedMessage: EncryptedMessage): Boolean {
        Logging.d(TAG, "onReceiveMessage [+] message <" + message + ">")
        // broadcast check for broadcasts to be added


//        if (message is BroadcastTextMessage) { // todo will this still work ?
//            var broadcast = BroadcastManager.getBroadcastById(message.getBroadcast().id).get()
//            if (broadcast == null) {
//                val allowed = SettingsManager.getBooleanSetting(getString(R.string.key_allow_broadcast_adding), this)
//                if (allowed) {
//                    broadcast = Broadcast(message.getBroadcast().id, message.getBroadcast().label)
//                    BroadcastManager.addBroadcast(broadcast)
//                    val default_add_all_users = SettingsManager.getBooleanSetting(getString(R.string.key_default_add_all_users), this)
//                    if (default_add_all_users) {
//                        BroadcastManager.addUsersToBroadcast(broadcast, UserManager.getAllUsers().get())
//                    }
//                    onBroadcastAdded(broadcast)
//                }
//                Logging.d(TAG, "onReceiveMessage [+] broadcast auto add disabled")
//            }
////            if (doForward) {
////                message.getEncryptedMessage()?.let {
////                    OnionTaskProcessor.enqueue(ForwardMessageTask(it))
////                } ?: kotlin.run {
////                    Logging.e(TAG, "onReceiveMeessage [-] unable to forward broadcast message")
////                }
////            } else {
////                Logging.d(TAG, "onReceiveMessage [+] message forwarding disabled")
////            }
//        }


        // todo add check if listeners are registered !?
        Logging.d(TAG, "onReceiveMessage [+] serviceClients <" + serviceClients + ">")
        var consumed = false
        serviceClients.forEach {
            if (it.onReceiveMessage(message, encryptedMessage)) {
                consumed = true
            }
        }
        if (!consumed) {
            if (SettingsManager.getBooleanSetting(getString(R.string.key_notifications), this)) {
                var tag = "New Messages"
                var text = "You have received new messages. Click to show them up"
                if (message is SymmetricMessage) {
                    tag = message.hashedFrom
                    UserManager.getUserByHashedId(message.hashedFrom).get()?.let { it ->
                        it.details?.let {
                            if (it.isNotEmpty()) {
                                it[0].alias.let {
                                    tag = it
                                }
                            }
                        }

                    }
                }
                if (message is IBroadcastMessage) {
                    tag += "@${message.getBroadcast().label}"
                }
                if (message is ITextMessage) {
                    text = message.getText().text
                }
                if (MessageTypes.shouldShowPush(encryptedMessage.type)) {
                    showNotification(tag, text, NOTIFICATION_CHANNEL_MESSAGES)
                }
            }
        }
        return true
    }

    fun deleteNotifications() { // todo this doesnt work !!
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_MESSAGES)
        } else {
            // todo fix ?
        }
    }

    fun sendMessage(
        message: IMessage,
        fromUID: String,
        to: Conversation
    ): OnionFuture<SendMessageTask.SendMessageResult> {
        return OnionTaskProcessor.enqueuePriority(SendMessageTask(message, to))
    }

    override fun onTaskEnqueued(task: Any) {
    }

    override fun onTaskFinished(task: Any, result: OnionTask.Result) {
    }

    fun onNetworkStatusChanged() {
        ConnectionManager.checkConnection()
    }

    override fun onConnectionStateChanged(state: ConnectionManager.ConnectionState) {
        serviceClients.forEach {
            it.onConnectionStateChanged(state)
        }
    }
}