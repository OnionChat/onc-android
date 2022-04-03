package com.onionchat.dr0id.connectivity

import android.content.Context
import com.onionchat.common.Logging
import com.onionchat.connector.ITorServiceControllerCallback
import com.onionchat.connector.OnReceiveClientDataListener
import com.onionchat.connector.http.OnionServer
import com.onionchat.connector.tor.TorNetworkConnectivityStatus
import com.onionchat.connector.tor.TorServiceConnectionState
import com.onionchat.connector.tor.TorServiceController
import com.onionchat.connector.tor.TorServiceInfo
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.OnionFuture
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.tasks.CheckConnectionTask
import com.onionchat.dr0id.queue.tasks.InitiateTorNetworkConnectionTask
import com.onionchat.dr0id.queue.tasks.PingTask
import com.onionchat.localstorage.userstore.User
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.*

object ConnectionManager {

    const val TAG = "ConnectionManager"


    fun checkConnection(): OnionFuture<CheckConnectionTask.CheckConnectionResult> {
        return StateMachine.checkConnection()
    }

    fun reconnect() { // : OnionFuture<InitiateTorNetworkConnectionTask.InitiateTorNetworkConnectionResult>
        //return OnionTaskProcessor.enqueuePriority(InitiateTorNetworkConnectionTask())
    }

    fun connect() : OnionFuture<InitiateTorNetworkConnectionTask.InitiateTorNetworkConnectionResult> {
        return StateMachine.connect()
    }

    @Deprecated("use user statemachine")
    fun pingUser(user: User, payload: PingPayload = PingPayload()): OnionFuture<PingTask.PingResult> {
        return OnionTaskProcessor.enqueuePriority(PingTask(user.id, payload))
    }

    fun pingMySelf(): OnionFuture<PingTask.PingResult> {
        return OnionTaskProcessor.enqueuePriority(PingTask())
    }

    fun newTorIdentity() : String {
        return TorServiceController.newCirciut()
    }

    fun getHostName(context: Context) : String {
        return TorServiceController.getHostName(context)
    }


    val listeners = ArrayList<WeakReference<ConnectionStateChangeListener>>()

    enum class ConnectionState {
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        ERROR
    }

    enum class ConnectionTestState {
        UNTESTED,
        TESTING,
        TESTED
    }

    private val receiveClientDataListeners = HashSet<OnReceiveClientDataListener>()

    fun registerOnReceiveCallback(onReceiveClientDataListener: OnReceiveClientDataListener) {
        receiveClientDataListeners.add(onReceiveClientDataListener)
    }

    fun registerConnectionStateChangeListener(connectionStateChangeListener: ConnectionStateChangeListener) {
        listeners.add(WeakReference(connectionStateChangeListener))
    }

    internal object StateMachine : OnionTaskProcessor.OnionTaskProcessorObserver, OnReceiveClientDataListener, ITorServiceControllerCallback {
        init {
            OnionTaskProcessor.addObserver(this)
        }
        var state = ConnectionState.DISCONNECTED // todo use atomic !?
            set(value) {
                Logging.d(TAG, "StateMachine [+] change connection state <${value}>")
                if(value != field) {
                    listeners?.forEach {
                        it.get()?.onConnectionStateChanged(value)
                    }
                }
                field = value
            }

        var testState = ConnectionTestState.UNTESTED
            private set(value) {
                Logging.d(TAG, "StateMachine [+] change test state <${value}>")
                field = value
            }
        override fun onTaskEnqueued(task: Any) {
            if (task is CheckConnectionTask) {
                testState = ConnectionTestState.TESTING
            } else if (task is InitiateTorNetworkConnectionTask) {
            }
        }

        override fun onTaskFinished(task: Any, result: OnionTask.Result) {
            if (task is CheckConnectionTask) {
                testState = ConnectionTestState.TESTED
                // todo process result and change state
                if (result.status != OnionTask.Status.SUCCESS) {
                    // todo connection failed... enqueue connection task

                    when (state) {
                        ConnectionState.DISCONNECTED -> {
                            // just enqueue connection task
                            Logging.e(TAG, "onTaskFinished [-] we're still not connected dude")
                            //connect()
                        }
                        ConnectionState.CONNECTED -> {
                            // we lost connection !!
                            Logging.e(TAG, "onTaskFinished [-] finally we lost connection.... let's try to reconnect!")
                        }
                    }
                    state = ConnectionState.DISCONNECTED
                } else {
                    state = ConnectionState.CONNECTED
                }
            } else if (task is InitiateTorNetworkConnectionTask) {
                if (result.status != OnionTask.Status.SUCCESS) {
                    // todo what to do here

                }

            }
        }


        fun reconnect() { // : OnionFuture<InitiateTorNetworkConnectionTask.InitiateTorNetworkConnectionResult>
            //return OnionTaskProcessor.enqueuePriority(InitiateTorNetworkConnectionTask())
        }

        fun connect(): OnionFuture<InitiateTorNetworkConnectionTask.InitiateTorNetworkConnectionResult> {
            Logging.d(TAG, "connect")
            return OnionTaskProcessor.enqueuePriority(InitiateTorNetworkConnectionTask(this, this))
        }

        fun checkConnection(): OnionFuture<CheckConnectionTask.CheckConnectionResult> {
            Logging.d(TAG, "checkConnection")
            return OnionTaskProcessor.enqueuePriority(CheckConnectionTask())
        }


        override fun onDownloadApk() {
            Logging.d(TAG, "onDownloadApk [+] someone is going to download our apk ;)")
            receiveClientDataListeners.forEach {
                it.onDownloadApk()
            }
        }

        override fun onReceive(type: OnionServer.ReceiveDataType, data: String) {
            Logging.d(TAG, "onReceive [+] type=$type, <messageSize = ${data.length}, listeners=${receiveClientDataListeners.size}>")
            receiveClientDataListeners.forEach {
                it.onReceive(type, data)
            }
        }

        override fun onStreamRequested(inputStream: InputStream): Boolean {
            Logging.d(TAG, "onStreamRequested [+] $inputStream")
            receiveClientDataListeners.forEach {
                val lsitener = it
                if (lsitener.onStreamRequested(inputStream)) {
                    return true
                }
            }
            return false
        }

        override fun onTorNetworkConnectivityStatusChanged(torNetworkConnectivitiyStatus: TorNetworkConnectivityStatus) {
            Logging.d(TAG, "onTorNetworkConnectivityStatusChanged [+] $torNetworkConnectivitiyStatus")
            when (torNetworkConnectivitiyStatus) {
                TorNetworkConnectivityStatus.STATUS_STARTING -> {
                    // todo what to do here

                }
                TorNetworkConnectivityStatus.STATUS_ON -> {
                    // todo what to do here
                    checkConnection()
                }
                TorNetworkConnectivityStatus.STATUS_STOPPING -> {
                    // todo what to do here

                }
                TorNetworkConnectivityStatus.STATUS_OFF -> {
                    // todo what to do here

                }
            }
        }

        override fun onTorServiceConnectionChanged(serviceConnectionState: TorServiceConnectionState?, connectionInfo: TorServiceInfo?) {
            Logging.d(TAG, "onTorServiceConnectionChanged [+] $serviceConnectionState, $connectionInfo")
            if(connectionInfo != null) {
                UserManager.myId = connectionInfo.hostname
            }
            when(serviceConnectionState) {
                TorServiceConnectionState.BOUND -> {
                    checkConnection()
                }
                TorServiceConnectionState.WAITING -> {

                }
                TorServiceConnectionState.UNBOUND -> {

                }
                TorServiceConnectionState.FAILURE -> {

                }
                null -> {

                }
            }
        }

    }
}

data class ConnectionInformation(val commonIp: String, val torIp: String)

interface ConnectionStateChangeListener {
    fun onConnectionStateChanged(state: ConnectionManager.ConnectionState)
}