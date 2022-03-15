package com.onionchat.dr0id

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.onionchat.common.Logging
import com.onionchat.connector.BackendConnector
import com.onionchat.connector.IConnectorCallback
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.queue.OnionFuture
import com.onionchat.dr0id.queue.tasks.SendMessageTask
import com.onionchat.dr0id.service.OnionChatConnectionService
import com.onionchat.dr0id.stream.StreamingWindow
import com.onionchat.dr0id.ui.contactdetails.ContactDetailsActivity
import com.onionchat.dr0id.ui.web.OnionWebActivity
import com.onionchat.localstorage.userstore.Broadcast
import com.onionchat.localstorage.userstore.User

abstract class OnionChatActivity : AppCompatActivity(), IConnectorCallback, OnionChatConnectionService.ServiceClient {

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
            mService.checkConnection(this@OnionChatActivity)
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
        startForegroundService(Intent(this, OnionChatConnectionService::class.java));
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        startForegroundService(Intent(this, OnionChatConnectionService::class.java));
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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBound) {
            try {
                unbindService(connection)
            } catch (e: Exception) {
                Logging.e(TAG, "Error while unbding background service", e)
            }
            mBound = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (mBound) {
            Logging.d(TAG, "onResume [+] addClient")
            mService.addClient(this@OnionChatActivity)
        } else {
            Logging.d(TAG, "onResume [-] unable to add client")
        }
    }

    override fun onPause() {
        super.onPause()
        if (mBound) {
            Logging.d(TAG, "onResume [+] removeClient")
            mService.removeClient(this@OnionChatActivity)
        } else {
            Logging.d(TAG, "onResume [-] unable to remove client")
        }
    }

    fun checkConnection() {
        if (mBound) {
            mService.checkConnection(this@OnionChatActivity)
        }
    }

    abstract override fun onConnected(success: Boolean)


    override fun onReceiveMessage(message: IMessage): Boolean {
        return false
    }

    override fun onPingReceived(data: String) {
    }

    fun applyNewCirciut() {
        val circuit = BackendConnector.getConnector().newCirciut()
        Logging.d("ContactListWindow", "Applied new circiut <" + circuit + ">");
        Toast.makeText(this, "APPLIED NEW CIRCUIT: " + circuit, Toast.LENGTH_SHORT).show()
    }


    enum class ConnectionStatus {
        CONNECTING,
        CONNECTED,
        ERROR,
        PINGING
    }


    fun updateConnectionState(connectionStatus: ConnectionStatus) {
        var message = ""
        var color = 0
        var duration = Snackbar.LENGTH_LONG
        when (connectionStatus) {
            ConnectionStatus.CONNECTING -> {
                message = "Connecting"
                color = Color.GREEN
                duration = Snackbar.LENGTH_INDEFINITE
            }
            ConnectionStatus.CONNECTED -> {
                message = "Connected"
                color = Color.GREEN
            }
            ConnectionStatus.ERROR -> {
                message = "Not Connected. Please try it later."
                color = Color.RED
                duration = Snackbar.LENGTH_INDEFINITE
            }
            ConnectionStatus.PINGING -> {
                message = "Pinging"
                color = Color.GREEN
            }
            else -> {
                message = "UNKNOWN ERROR."
                color = Color.RED
            }
        }
        val coordinatorLayout = findViewById<View>(R.id.coordinatorLayout) as CoordinatorLayout
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
        openOnionLinkInWebView("http://" + user.id + "/web/index.html", user.getHashedId())
    }
    fun openStreamWindow(user: User) {
        val intent = Intent(this, StreamingWindow::class.java)
        intent.putExtra(StreamingWindow.EXTRA_CONVERSATION_ID, user.id)
        startActivity(intent)
    }

    fun openOnionLinkInWebView(url: String, username: String? = null) {
        val intent = Intent(this, OnionWebActivity::class.java)
        intent.putExtra(OnionWebActivity.EXTRA_URL, url)
        username?.let {
            intent.putExtra(OnionWebActivity.EXTRA_USERNAME, it)
        }
        startActivity(intent)
    }

    fun deleteNotifications() {
        if (mBound) {
            mService.deleteNotifications()
        }
    }


    fun openBroadcastDetails(id: String, resultLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(this, ContactDetailsActivity::class.java)
        intent.putExtra(ContactDetailsActivity.EXTRA_BROADCAST_ID, id)
        resultLauncher.launch(intent)
    }

    fun openContactDetails(uid: String, resultLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(this, ContactDetailsActivity::class.java)
        intent.putExtra(ContactDetailsActivity.EXTRA_CONTACT_ID, uid)
        resultLauncher.launch(intent)
    }

    fun sendMessage(message: IMessage, fromUID: String, to: User): OnionFuture<SendMessageTask.SendMessageResult>? {
        if (mBound) {
            return mService.sendMessage(message, fromUID, to)
        }
        Logging.e(TAG, "sendMessage [-] service is not bounr [-] invalid state")
        return null
    }
}