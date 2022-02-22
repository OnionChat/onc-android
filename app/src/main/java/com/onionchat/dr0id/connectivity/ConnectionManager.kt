package com.onionchat.dr0id.connectivity

import android.content.Context
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.common.SettingsManager
import com.onionchat.connector.BackendConnector
import com.onionchat.connector.Communicator
import com.onionchat.connector.IConnectorCallback
import com.onionchat.connector.http.HttpServerSettings
import com.onionchat.dr0id.R
import com.onionchat.dr0id.users.UserManager
import com.onionchat.localstorage.userstore.User
import java.util.concurrent.Executors

object ConnectionManager {

    var executorService = Executors.newFixedThreadPool(1)
    var messageSendService = Executors.newCachedThreadPool()

    fun checkConnection(context: Context, connectorCallback: IConnectorCallback) {
        executorService.submit {
            Logging.d("ConnectionManager", "checkConnection")
            if (BackendConnector.getConnector().isConnected) {
                Logging.d("ConnectionManager", "checkConnection - call onConnected callback")
                connectorCallback.onConnected(true)
            } else {
                BackendConnector.getConnector().connect(context, object : IConnectorCallback {
                    override fun onConnected(success: Boolean) {
                        //val c = Communicator()
                        //c.uploadData(getUniqueId(this@MainActivity), Build.HARDWARE + ";" + Build.MANUFACTURER)
                        Logging.d("ConnectionManager", "onConnected - call onConnected callback")
                        connectorCallback.onConnected(success)
                    }
                }, HttpServerSettings(SettingsManager.getBooleanSetting(context.getString(R.string.key_enable_web), context)))
            }
            Logging.d("ConnectionManager", "checkConnection done")
        }.get()
    }

    fun isUserOnline(user : User, callback : (Boolean) -> Unit)  {
        UserManager.myId?.let {
            Thread {
                callback(Communicator.isUserOnline(user.id, IDGenerator.toVisibleId(it)).get())
            }.start()
        }
    }
}