package com.onionchat.connector.tor

import android.content.*
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.system.ErrnoException
import android.system.Os
import com.onionchat.common.Logging
import com.onionchat.common.Logging.d
import com.onionchat.common.Logging.e
import com.onionchat.connector.ITorServiceControllerCallback
import com.onionchat.connector.R
import com.onionchat.connector.ServerTools
import info.guardianproject.netcipher.NetCipher
import net.freehaven.tor.control.TorControlCommands
import org.apache.commons.io.FileUtils
import org.torproject.jni.TorService
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException


data class TorServiceInfo(val httpTunnel: Int, val socksPort: Int, val hostname: String)

data class TorServiceControllerSettings(val onionServerPort: Int)

enum class TorServiceConnectionState {
    BOUND,
    WAITING,
    UNBOUND,
    FAILURE
}

enum class TorNetworkConnectivityStatus {
    STATUS_STARTING,
    STATUS_ON,
    STATUS_STOPPING,
    STATUS_OFF
}

// todo remove unused code
object TorServiceController {
    var torService: TorService? = null

    //    private File defaultsTorrc;
    //    private File torrc;
    private var mBinder: TorService.LocalBinder? = null
    private var connected = false
    var httpTunnelPort = 0
    var socksPort = 0

    var torServiceControllerCallback: ITorServiceControllerCallback? = null


    private fun registerReceiver(context: Context): Boolean {


        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
//                if (!TorService.ACTION_STATUS.equals(intent.getAction())) {
//                    Logging.d(TAG, "!TorService.ACTION_STATUS.equals(intent.getAction())");
//                    return;
//                }
                val torServiceCallback = torServiceControllerCallback
                if (torServiceCallback == null) {
                    Logging.e(TAG, "onReceive [-] !! WARNING !! no torServiceCallback was set")
                }
                val status = intent.getStringExtra(TorService.EXTRA_STATUS)

                d(TAG, "receiver.onReceive: $status $intent")
                if (TorService.STATUS_STARTING == status) {
                    torServiceCallback?.onTorNetworkConnectivityStatusChanged(TorNetworkConnectivityStatus.STATUS_STARTING)
                } else if (TorService.STATUS_ON == status) {
                    torServiceCallback?.onTorNetworkConnectivityStatusChanged(TorNetworkConnectivityStatus.STATUS_ON)
                    onTorServiceUp()
                } else if (TorService.STATUS_STOPPING == status) {
                    torServiceCallback?.onTorNetworkConnectivityStatusChanged(TorNetworkConnectivityStatus.STATUS_STOPPING)
                } else if (TorService.STATUS_OFF == status) {
                    torServiceCallback?.onTorNetworkConnectivityStatusChanged(TorNetworkConnectivityStatus.STATUS_OFF)
                    connected = false
                } else {
                    throw IllegalStateException("UNKNOWN STATUS FROM INTENT: $intent")
                }
            }
        }
        val handlerThread = HandlerThread(receiver.javaClass.simpleName)
        handlerThread.start()
        val looper = handlerThread.looper
        val handler = Handler(looper)
        context.registerReceiver(receiver, IntentFilter(TorService.ACTION_STATUS), null, handler)
        return true
    }

    fun onTorServiceUp() {
        Thread {
            val torService = torService
            if (torService == null) {
                Logging.e(TAG, "torService is null... waiting for service connection")
                return@Thread
            }
            if (!checkPorts()) {
                Logging.e(TAG, "onServiceConnected [-] checkPorts failed !!")
//                torServiceControllerCallback?.onTorServiceConnectionChanged(TorServiceConnectionState.FAILURE, null)
            }

            if (!setupNetcipher()) {
                Logging.e(TAG, "onServiceConnected [-] setupNetcipher failed !!")
                torServiceControllerCallback?.onTorServiceConnectionChanged(TorServiceConnectionState.FAILURE, null)
            }
            Logging.d(TAG, "onServiceConnected [+] TorService bound")
            torServiceControllerCallback?.onTorServiceConnectionChanged(
                TorServiceConnectionState.BOUND,
                TorServiceInfo(
                    torService.httpTunnelPort, torService.socksPort,
                    getHostName(torService)
                )
            )
        }.start()
    }

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            if (service is TorService.LocalBinder) {
                mBinder = service as TorService.LocalBinder
                //            mBound = true;
                if (mBinder == null) {
                    Logging.e(TAG, "onServiceConnected [-] mBinder is null")
                    torServiceControllerCallback?.onTorServiceConnectionChanged(TorServiceConnectionState.FAILURE, null)
                }
                torService = mBinder!!.service
                val torService = torService
                if (torService == null) {
                    Logging.e(TAG, "onServiceConnected [-] torService is null")
                    torServiceControllerCallback?.onTorServiceConnectionChanged(TorServiceConnectionState.FAILURE, null)
                } else {
                    if (torService.socksPort > 0) {
                        onTorServiceUp()
                    }
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
//            mBound = false;
            Logging.e(TAG, "onServiceDisconnected [-] $arg0")
        }
    }

    private fun establishServiceConnection(context: Context): Boolean {
        val serviceIntent = Intent(context, TorService::class.java)
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        return true
    }

    private fun checkPorts(): Boolean {
//        socksPort = torService!!.socksPort
        d(TAG, "checkPorts [+] port <$socksPort>")
        //assertTrue(isServerSocketInUse(socksPort));
        if (!ServerTools.isServerSocketInUse(socksPort)) {
            e(TAG, "isServerSocketInUse [-] socksPort not in use")
            return false
        }
        //        if (socksPort != 9050) {
//            //assertFalse("Something else is providing port 9050!", isServerSocketInUse(9050));
//            Logging.e(TAG, "isServerSocketInUse [-] Something else is providing port 9050! <"+socksPort+">");
//
//            return false;
//        }
//        httpTunnelPort = torService!!.httpTunnelPort
        d(TAG, "checkPorts [+] port <$httpTunnelPort>")
        if (!ServerTools.isServerSocketInUse(httpTunnelPort)) {
            e(TAG, "isServerSocketInUse [-] httpTunnelPort not in use")
            return false
        }
        //        if (httpTunnelPort != 8118) {
//            //assertFalse("Something else is providing port 8118!", isServerSocketInUse(8118));
//            Logging.e(TAG, "isServerSocketInUse [-] Something else is providing port 8118");
//            return false;
//        }
        return true
    }

    private fun setupNetcipher(): Boolean {
        Logging.d(TAG, "setupNetcipher [+] $httpTunnelPort")
        NetCipher.setProxy("localhost", httpTunnelPort)
//        if (Build.VERSION.SDK_INT < 30) {
//            if (!NetCipher.isNetCipherGetHttpURLConnectionUsingTor()) {
//                e(TAG, "\"NetCipher.getHttpURLConnection should use Tor\"")
//                return false
//            }
//        }
        return true
    }


    fun getServiceDir(context: Context): File {
        return File(context.filesDir.absolutePath + "/onc_host/")
    }

    @Throws(IOException::class)
    fun getHostName(context: Context): String {
        val f = File(getServiceDir(context).absolutePath + "/hostname")
        val reader = BufferedReader(FileReader(f))
        val line = reader.readLine()
        reader.close()
        return line
    }


    fun newCirciut(): String {
        try {
            return mBinder!!.service.torControlConnection.extendCircuit("0", "")
        } catch (e: IOException) {
            e("TorConnector", "Apply new circuit failed", e)
        }
        return "Apply new circuit failed"
    }

    fun getInfo(): String { // improve to fetch circiut
        try {
            return mBinder!!.service.torControlConnection.getInfo("ns/all")
        } catch (e: IOException) {
            e(TAG, "Error while retrieve TOR info", e)
        }
        return ""
    }

    fun reconnect(context: Context): Boolean { // todo shutdown service !!
        d(TAG, "Going to restart TOR")
        try {
            mBinder!!.service.torControlConnection.signal(TorControlCommands.SIGNAL_RELOAD)
            mBinder!!.service.torControlConnection.signal(TorControlCommands.SIGNAL_CLEARDNSCACHE)
            mBinder!!.service.torControlConnection.signal(TorControlCommands.SIGNAL_HEARTBEAT)
            mBinder!!.service.torControlConnection.signal(TorControlCommands.SIGNAL_NEWNYM)
            mBinder!!.service.torControlConnection.signal(TorControlCommands.SIGNAL_ACTIVE)
            /*
            SIGNAL_HEARTBEAT
SIGNAL_ACTIVE
             */
        } catch (e: IOException) {
            e(TAG, "Error while restart TOR", e)
            return false
        }
        return true
    }

    @Throws(IOException::class, ErrnoException::class)
    fun writeTorConfig(context: Context, onionServerPort: Int) {
        val torrc = TorService.getTorrc(context)
        val defaultsTorrc = TorService.getDefaultsTorrc(context)
        val serviceDir = getServiceDir(context)
        serviceDir.mkdir()
        Os.chmod(serviceDir.absolutePath, 448) // 700 in octal
        httpTunnelPort = ServerTools.getRandomPort(context.resources.getInteger(R.integer.http_tunnel_proposal))
        socksPort = ServerTools.getRandomPort(context.resources.getInteger(R.integer.socks_tunnel_proposal))
        val configData = """
            HTTPTunnelPort ${httpTunnelPort}
            SocksPort ${socksPort}
            HiddenServiceDir ${serviceDir.absolutePath}
            HiddenServicePort 80 127.0.0.1:$onionServerPort
            """.trimIndent()
        Logging.d(TAG, "writeTorConfig [+] write config data $configData")
//        String configData = "DNSPort auto\n";
        FileUtils.write(defaultsTorrc, configData)
        FileUtils.write(torrc, configData)

//        Logging.d(TAG, "TORRC: LOCATION <" + configFile.getAbsolutePath() + "> ");
//        if (!configFile.exists()) {
//            Logging.d(TAG, "TORRC: DOESN'T EXIST <" + configFile.getAbsolutePath() + "> create new one");
//            Os.chmod(serviceDir.getAbsolutePath(), 700);
//            BufferedWriter write = new BufferedWriter(new FileWriter(configFile));
//            write.write();
//            write.close();
//            return;
//        }
//        BufferedReader reader = new BufferedReader(new FileReader(configFile));
//        String line;
//        while ((line = reader.readLine()) != null) {
//            Logging.d(TAG, "TORRC: <" + line + ">");
//        }
//        reader.close();
    }
//
//    fun bind(context: Context, torServiceCallback: ITorServiceCallback, serverSettings: OnionServerSettings): Boolean {
//        startOnionServer(object : OnionServerCallback {
//
//
//            override fun onDownloadApk(): String {
//                return context.applicationInfo.sourceDir
//            }
//
//            override fun onBound(port: Int) {
//                // write config
//                try {
//                    writeTorConfig(context, port) // todo write custom tor port !! We don't want conflicts with orbot !!
//                } catch (e: IOException) {
//                    e(TAG, "Unable to write tor configuration", e)
//                    torServiceCallback.onTorNetworkConnectivityStatusChanged(false)
//                    return
//                } catch (e: ErrnoException) {
//                    e(TAG, "Unable to write tor configuration", e)
//                    torServiceCallback.onTorNetworkConnectivityStatusChanged(false)
//                    return
//                }
//
//                // finally start the tor service
//                registerReceiver(context) { success: Boolean ->
//                    try {
//                        var res = true
//                        if (!checkPorts()) {
//                            e(TAG, " UNABLE TO ESTABLISH CONNECTION! PORTS COULD NOT BE SETUP")
//                            res = false
//                        }
//                        if (!setupNetcipher()) {
//                            e(TAG, "Unable to setup netcipher")
//                            res = false
//                        }
//                        //                        try {
//                        //                            if (!testConnection()) {
//                        //                                e(TAG, "TestConnection failed")
//                        //                                res = false
//                        //                            }
//                        //                        } catch (e: IOException) {
//                        //                            e(TAG, "TestConnection failed", e)
//                        //                            res = false
//                        //                        }
//                        val hostname = getHostName(context)
//                        d(TAG, "Hostname <$hostname>")
//                        if (hostname == null || hostname.isEmpty()) {
//                            res = false
//                        }
//                        newCirciut()
//                        torServiceCallback.onTorNetworkConnectivityStatusChanged(res)
//                        connected = res
//                    } catch (e: Exception) {
//                        e(TAG, "Error while connect TOR", e)
//                    }
//                }
//                registerReceiver(context)
//            }
//
//            override fun onFail(error: Exception?) {
//                e(TAG, "Error while input server", error!!)
//                torServiceCallback.onTorNetworkConnectivityStatusChanged(false)
//            }
//        }, mOnReceiveClientDataListener, serverSettings)
//        return false
//    }

    @Throws(IOException::class, ErrnoException::class)
    fun bind(context: Context, torConnectorSettings: TorServiceControllerSettings, torServiceControllerCallback: ITorServiceControllerCallback): Boolean {
        Logging.d(TAG, "Going to bind tor service")
        // write config
        writeTorConfig(context, torConnectorSettings.onionServerPort) // todo write custom tor port !! We don't want conflicts with orbot !!
        this.torServiceControllerCallback = torServiceControllerCallback
        registerReceiver(context)
        establishServiceConnection(context)
        return true
    }

    fun isConnected(): Boolean {
        return connected
    }

    const val TAG = "TorServiceController"


//    companion object {
//
//
//        @Throws(IOException::class)
//        private fun checkIsTor(connection: URLConnection): Boolean {
//            var isTor = false
//            d("NetCipher", "content length: " + connection.contentLength)
//            val jsonReader = JsonReader(InputStreamReader(connection.getInputStream()))
//            jsonReader.beginObject()
//            while (jsonReader.hasNext()) {
//                val name = jsonReader.nextName()
//                if ("IsTor" == name) {
//                    isTor = jsonReader.nextBoolean()
//                    break
//                }
//                jsonReader.skipValue()
//            }
//            return isTor
//        }
//    }
}
