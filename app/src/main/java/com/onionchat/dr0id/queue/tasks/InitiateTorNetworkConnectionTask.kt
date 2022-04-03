package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Logging
import com.onionchat.common.SettingsManager
import com.onionchat.connector.ITorServiceControllerCallback
import com.onionchat.connector.OnReceiveClientDataListener
import com.onionchat.connector.http.OnionServer
import com.onionchat.connector.http.OnionServerInformation
import com.onionchat.connector.http.OnionServerSettings
import com.onionchat.connector.tor.TorServiceControllerSettings
import com.onionchat.connector.tor.TorServiceController
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.localstorage.PathProvider
import com.onionchat.localstorage.PathProvider.getWebDir

class InitiateTorNetworkConnectionTask(val onReceiveClientDataListener: OnReceiveClientDataListener,  val torServiceControllerCallback: ITorServiceControllerCallback) :
    OnionTask<InitiateTorNetworkConnectionTask.InitiateTorNetworkConnectionResult>() {


    class InitiateTorNetworkConnectionResult(status: Status, onionServerInformation: OnionServerInformation? = null, exception: Exception? = null) :
        OnionTask.Result(status, exception) {
    }

    override fun onUnhandledException(exception: Exception): InitiateTorNetworkConnectionTask.InitiateTorNetworkConnectionResult {
        return InitiateTorNetworkConnectionTask.InitiateTorNetworkConnectionResult(status = Status.FAILURE, exception = exception)
    }

    companion object {
        val TAG = "ConnectTask"
    }

    override fun run(): InitiateTorNetworkConnectionResult {
//        BackendConnector.connector.connect()
        if(ConnectionManager.StateMachine.state == ConnectionManager.ConnectionState.CONNECTING) {
            Logging.d(TAG, "run [-] connection task already running... abort")
            return InitiateTorNetworkConnectionResult(Status.PENDING)
        }
        ConnectionManager.StateMachine.state = ConnectionManager.ConnectionState.CONNECTING
        val context = context
        if (context == null) {
            Logging.d(TAG, "run [-] cannot connect.. context is null. We set the state to pending... we should try it again later...")
            return InitiateTorNetworkConnectionResult(Status.PENDING)
        }

        val information = OnionServer.startOnionServer(onReceiveClientDataListener,
            OnionServerSettings(
                SettingsManager.getBooleanSetting(context.getString(R.string.key_enable_web), context),
                PathProvider.getAttachmentPath(context),
                PathProvider.getWebDir(context),
                PathProvider.getApkPath(context)
                )
            )
        Logging.d(TAG, "run [+] successfully started OnionServer <${information}>")
        val torServiceControllerSettings = TorServiceControllerSettings(information.port)
        val bindingStarted = TorServiceController.bind(context, torServiceControllerSettings, torServiceControllerCallback)
        if(!bindingStarted) {
            return InitiateTorNetworkConnectionResult(Status.FAILURE, onionServerInformation = information)
        }
        Logging.d(TAG, "run [+] successfully started TorService <${bindingStarted}>")

//        val torConnectionResult = BackendConnector.CONNECTOR.connect(context,
//            HttpServerSettings(
//                SettingsManager.getBooleanSetting(context.getString(R.string.key_enable_web), context),
//                PathProvider.getAttachmentPath(context),
//                getWebDir(context))
//        )
        return InitiateTorNetworkConnectionResult(Status.SUCCESS, onionServerInformation = information)
    }
}