package com.onionchat.dr0id

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.onionchat.common.AddUserPayload
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.connectivity.PingPayload
import com.onionchat.dr0id.connectivity.PingPayload.Companion.PURPOSE_STREAM
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.queue.OnionFuture
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.tasks.CheckConnectionTask
import com.onionchat.dr0id.queue.tasks.SendMessageTask
import com.onionchat.dr0id.service.OnionChatConnectionService
import com.onionchat.dr0id.ui.ActivityLauncher
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer.showError
import com.onionchat.localstorage.messagestore.EncryptedMessage
import com.onionchat.localstorage.userstore.Broadcast
import com.onionchat.localstorage.userstore.User
import java.io.InputStream

abstract class OnionChatActivity : AppCompatActivity(), OnionChatConnectionService.ServiceClient {

    private lateinit var mService: OnionChatConnectionService
    private var mBound: Boolean = false

    companion object {
        val TAG = "OnionChatActivity"
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Logging.d(TAG, "onServiceConnected")

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as OnionChatConnectionService.LocalBinder
            mService = binder.getService()
            mBound = true
            mService.addClient(this@OnionChatActivity)
            Logging.d(TAG, "ask service for connectivity status")
            mService.checkConnection().then {
                onCheckConnectionFinished(it)
            }
        }

        override fun onBindingDied(name: ComponentName?) {
            Logging.d(TAG, "onBindingDied")
            super.onBindingDied(name)
            mBound = false
        }

        override fun onNullBinding(name: ComponentName?) {
            Logging.d(TAG, "onNullBinding")
            super.onNullBinding(name)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Logging.d(TAG, "onServiceDisconnected")
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logging.d(TAG, "startForegroundService")
        startService(Intent(this, OnionChatConnectionService::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, OnionChatConnectionService::class.java))
        } else {
            startService(Intent(this, OnionChatConnectionService::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        Intent(this, OnionChatConnectionService::class.java).also { intent ->
            if (!this.applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                Logging.e(TAG, "unable to connect to service")
            }
        }
    }

    override fun onStop() {
        super.onStop()
//        if(mBound) {
//            unbindService(connection)
//            mBound = false
//        }
//        if (mBound) {
//
//        } else {
//            Logging.d(TAG, "onResume [-] unable to remove client")
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBound) {
            Logging.d(TAG, "onDestroy [+] removeClient")
            mService.removeClient(this@OnionChatActivity)
            try {
                unbindService(connection)
            } catch (e: Exception) {
                Logging.e(TAG, "Error while unbding background service", e)
            }
            mBound = false
        }
    }

    var isTopActivity = false
    override fun onResume() {
        super.onResume()
        if (mBound) {
            Logging.d(TAG, "onResume [+] addClient")
            mService.addClient(this@OnionChatActivity)
        } else {
            Logging.d(TAG, "onResume [-] unable to add client")
        }
        isTopActivity = true
    }

    override fun onPause() {
        super.onPause()
        isTopActivity = false
    }

    fun checkConnection() {
        if (mBound) {
            mService.checkConnection()
        }
    }

    override fun onConnectionStateChanged(state: ConnectionManager.ConnectionState) {
    }

    abstract fun onCheckConnectionFinished(status: CheckConnectionTask.CheckConnectionResult)


    override fun onReceiveMessage(message: IMessage?, encryptedMessage: EncryptedMessage): Boolean {
        return false
    }

    override fun onPingReceived(pingPayload: PingPayload): Boolean {
        try {

            if (pingPayload.purpose == PURPOSE_STREAM) {// todo move to activity launcher class
                UserManager.getUserByHashedId(pingPayload.uid).get()?.let {
                    openStreamWindow(it, true)
                }
            }
        } catch (exception: java.lang.Exception) {
            Logging.e(TAG, "onPingReceived [+] unable to handle ping", exception)
        }
        return true
    }

    override fun onStreamRequested(inputStream: InputStream): Boolean {
        return false
    }

    @Deprecated("mirgrate to user online status state machine")
    fun startRecursivePing(tries: Int = 4, user: User, purpose: PingPayload = PingPayload()) {
        Logging.d(TAG, "startRecursivePing ${user.id}")
        if (tries <= 0) {
            onConversationOnline(false)
//            runOnUiThread {
//                updateConnectionState(ConnectionStatus.ERROR)
//            }
            return
        } else {
            ConnectionManager.pingUser(user, purpose).then {
                if(it.status == OnionTask.Status.SUCCESS) {
                    onConversationOnline(true)
//                    runOnUiThread {
//                        updateConnectionState(ConnectionStatus.CONNECTED)
//                    }
                } else {
                    startRecursivePing(tries - 1, user, purpose)
                }
            }
        }
    }

    open fun onConversationOnline(online: Boolean) {

    }

    fun newTorIdentity() {
        val res = ConnectionManager.newTorIdentity()
        Logging.d("ContactListWindow", "Applied new circiut <" + res + ">");
        Toast.makeText(this, "APPLIED NEW CIRCUIT: " + res, Toast.LENGTH_SHORT).show()
    }



    fun updateConnectionState(state: ConnectionManager.ConnectionState) {
        var message = ""
        var color = 0
        var duration = Snackbar.LENGTH_LONG
        when(state) {
            ConnectionManager.ConnectionState.CONNECTING -> {
                message = "Connecting"
                color = Color.GREEN
                duration = Snackbar.LENGTH_INDEFINITE
            }
            ConnectionManager.ConnectionState.CONNECTED -> {
                message = "Connected"
                color = Color.GREEN
            }
            ConnectionManager.ConnectionState.DISCONNECTED -> {
                message = "Connecting"
                color = Color.GREEN
                duration = Snackbar.LENGTH_INDEFINITE
            }
            ConnectionManager.ConnectionState.ERROR -> {

                message = "Not Connected. Please try it later."
                color = Color.RED
                duration = Snackbar.LENGTH_INDEFINITE
            }
            else -> {
                message = "UNKNOWN ERROR."
                color = Color.RED
            }
        }

        val coordinatorLayout = findViewById<View>(R.id.coordinatorLayout) as CoordinatorLayout? ?: return
        val snackbar = Snackbar
            .make(coordinatorLayout, message, duration)
        val snackBarLayout = snackbar.getView() as Snackbar.SnackbarLayout
        for (i in 0 until snackBarLayout.getChildCount()) {
            val parent: View = snackBarLayout.getChildAt(i)
            parent.rotation = 180f
        }
        snackbar.setBackgroundTint(color)
        snackbar.setTextColor(Color.BLACK)
        snackbar.show()
    }


    override fun onBroadcastAdded(broadcast: Broadcast) {
    }

    fun openContactWebSpace(user: User) {
        openOnionLinkInWebView(user.id)
    }

    fun openContactWebSpace(userId: String) {
        openOnionLinkInWebView("http://" + userId + "/web/index.html", IDGenerator.toHashedId(userId))
    }

    fun openStreamWindow(user: User, incomming: Boolean = false) {
        ActivityLauncher.openStreamWindow(user, incomming, this)
    }

    fun openOnionLinkInWebView(url: String, username: String? = null) {
        ActivityLauncher.openOnionLinkInWebView(url, username, this)
    }

    fun deleteNotifications() {
        if (mBound) {
            mService.deleteNotifications()
        }
    }

    fun openStatsActivity(user: User) {
        ActivityLauncher.openStatsActivity(user, this)
    }

    fun openImageImporter(resultLauncher: ActivityResultLauncher<Intent>) {
        ActivityLauncher.openImageImporter(null, false, resultLauncher, this)
    }

    fun openImageImporter(uri: Uri?, autoSelectFeed: Boolean = false, resultLauncher: ActivityResultLauncher<Intent>) {
        ActivityLauncher.openImageImporter(uri, autoSelectFeed, resultLauncher, this)
    }

    fun openVideoGallery(resultLauncher: ActivityResultLauncher<Intent>) {
        ActivityLauncher.openVideoGallery(resultLauncher)
    }

    fun openGallery(resultLauncher: ActivityResultLauncher<Intent>) {
        ActivityLauncher.openGallery(resultLauncher, this)
    }

    fun openTakePhoto(resultLauncher: ActivityResultLauncher<Intent>) {

        if (!ActivityLauncher.openTakePhoto(resultLauncher, this)) {
            showError(this, getString(R.string.error_cannot_open_camera), ErrorViewer.ErrorCode.CAMERA_INTENT_NOT_FOUND)
        }
    }

    fun openBroadcastDetails(id: String, resultLauncher: ActivityResultLauncher<Intent>) {
        ActivityLauncher.openBroadcastDetails(id, resultLauncher, this)
    }

    fun openContactDetails(uid: String, resultLauncher: ActivityResultLauncher<Intent>) {
        ActivityLauncher.openContactDetails(uid, resultLauncher, this)
    }

    fun openContactAddConfirmation(payload: AddUserPayload, resultLauncher: ActivityResultLauncher<Intent>) {
        ActivityLauncher.openContactAddConfirmation(payload, resultLauncher, this)
    }

    fun sendMessage(message: IMessage, fromUID: String, to: User): OnionFuture<SendMessageTask.SendMessageResult>? {
        return sendMessage(message, fromUID, Conversation(user = to))
    }

    fun sendMessage(message: IMessage, fromUID: String, to: Conversation): OnionFuture<SendMessageTask.SendMessageResult>? {
        if (mBound) {
            return mService.sendMessage(message, fromUID, to)
        }
        Logging.e(TAG, "sendMessage [-] service is not bounr [-] invalid state")
        return null
    }
}