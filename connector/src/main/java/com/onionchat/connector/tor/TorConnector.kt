package com.onionchat.connector.tor

import android.content.*
import android.os.*
import com.onionchat.common.Logging.d
import com.onionchat.common.Logging.e
import com.onionchat.connector.http.HttpServer.Companion.startService
import com.onionchat.connector.WebHelper.getWebDir
import com.onionchat.connector.OnReceiveClientDataListener
import com.onionchat.connector.IConnector
import org.torproject.jni.TorService
import com.onionchat.connector.IConnectorCallback
import com.onionchat.connector.tor.TorConnector
import com.onionchat.connector.ServerTools
import info.guardianproject.netcipher.NetCipher
import net.freehaven.tor.control.TorControlCommands
import android.system.ErrnoException
import android.system.Os
import android.util.JsonReader
import com.onionchat.connector.http.HttpServerSettings
import com.onionchat.connector.http.HttpServer.HttpServerCallback
import com.onionchat.connector.WebHelper
import com.onionchat.connector.http.HttpServer.ReceiveDataType
import org.apache.commons.io.FileUtils
import java.io.*
import java.lang.Exception
import java.lang.IllegalStateException
import java.net.URLConnection

class TorConnector(private val mOnReceiveClientDataListener: OnReceiveClientDataListener) : IConnector {
    private var torService: TorService? = null

    //    private File defaultsTorrc;
    //    private File torrc;
    private var mBinder: TorService.LocalBinder? = null
    private var connected = false
    var _httpTunnelPort = 0
    var _socksPort = 0
    private fun registerReceiver(context: Context, connectorCallback: IConnectorCallback): Boolean {
        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
//                if (!TorService.ACTION_STATUS.equals(intent.getAction())) {
//                    Logging.d(TAG, "!TorService.ACTION_STATUS.equals(intent.getAction())");
//                    return;
//                }
                val status = intent.getStringExtra(TorService.EXTRA_STATUS)
                d(TAG, "receiver.onReceive: $status $intent")
                if (TorService.STATUS_STARTING == status) {
                    //startingLatch.countDown();
                } else if (TorService.STATUS_ON == status) {
                    //startedLatch.countDown();
                    connectorCallback.onConnected(true)
                } else if (TorService.STATUS_STOPPING == status) {
                    //stoppingLatch.countDown();
                } else if (TorService.STATUS_OFF == status) {
                    //stoppedLatch.countDown();
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

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mBinder = service as TorService.LocalBinder
            //            mBound = true;
            torService = mBinder!!.service
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
//            mBound = false;
        }
    }

    private fun establishServiceConnection(context: Context): Boolean {
        val serviceIntent = Intent(context, TorService::class.java)
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        return true
    }

    private fun checkPorts(): Boolean {
        _socksPort = torService!!.socksPort
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
        _httpTunnelPort = torService!!.httpTunnelPort
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
        NetCipher.setProxy("localhost", httpTunnelPort)
        if (Build.VERSION.SDK_INT < 30) {
            if (!NetCipher.isNetCipherGetHttpURLConnectionUsingTor()) {
                e(TAG, "\"NetCipher.getHttpURLConnection should use Tor\"")
                return false
            }
        }
        return true
    }

    @Throws(IOException::class)
    private fun testConnection(): Boolean {
        return if (Build.VERSION.SDK_INT < 30) {
            checkIsTor(NetCipher.getHttpsURLConnection("https://check.torproject.org/api/ip"))
        } else true
    }

    fun getServiceDir(context: Context): File {
        return File(context.filesDir.absolutePath + "/onc_host/")
    }

    @Throws(IOException::class)
    override fun getHostName(context: Context): String {
        val f = File(getServiceDir(context).absolutePath + "/hostname")
        val reader = BufferedReader(FileReader(f))
        val line = reader.readLine()
        reader.close()
        return line
    }

    override fun getHttpTunnelPort(): Int {
        return _httpTunnelPort
    }

    override fun getSocksPort(): Int {
        return _socksPort
    }

    override fun newCirciut(): String {
        try {
            return mBinder!!.service.torControlConnection.extendCircuit("0", "")
        } catch (e: IOException) {
            e("TorConnector", "Apply new circuit failed", e)
        }
        return ""
    }

    override fun getInfo(): String { // improve to fetch circiut
        try {
            return mBinder!!.service.torControlConnection.getInfo("ns/all")
        } catch (e: IOException) {
            e(TAG, "Error while retrieve TOR info", e)
        }
        return ""
    }

    override fun reconnect(context: Context): Boolean {
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
    fun writeTorConfig(context: Context, port: Int) {
        val torrc = TorService.getTorrc(context)
        val defaultsTorrc = TorService.getDefaultsTorrc(context)
        val serviceDir = getServiceDir(context)
        serviceDir.mkdir()
        Os.chmod(serviceDir.absolutePath, 448) // 700 in octal
        val configData = """
            HiddenServiceDir ${serviceDir.absolutePath}
            HiddenServicePort 80 127.0.0.1:$port
            
            """.trimIndent()

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

    override fun connect(context: Context, connectorCallback: IConnectorCallback, serverSettings: HttpServerSettings): Boolean {
        startService(object : HttpServerCallback {
            override fun getWebFolder(): String {
                val webfolder = getWebDir(context)
                webfolder.mkdirs()
                d("TorConnector", "Use WebFolder <$webfolder>")
                return webfolder.absolutePath
            }

            override fun onDownloadApk(): String {
                return context.applicationInfo.sourceDir
            }

            override fun onBound(port: Int) {
                // write config
                try {
                    writeTorConfig(context, port)
                } catch (e: IOException) {
                    e(TAG, "Unable to write tor configuration", e)
                    connectorCallback.onConnected(false)
                    return
                } catch (e: ErrnoException) {
                    e(TAG, "Unable to write tor configuration", e)
                    connectorCallback.onConnected(false)
                    return
                }

                // finally start the tor service
                registerReceiver(context) { success: Boolean ->
                    try {
                        var res = true
                        if (!checkPorts()) {
                            e(TAG, " UNABLE TO ESTABLISH CONNECTION! PORTS COULD NOT BE SETUP")
                            res = false
                        }
                        if (!setupNetcipher()) {
                            e(TAG, "Unable to setup netcipher")
                            res = false
                        }
                        try {
                            if (!testConnection()) {
                                e(TAG, "TestConnection failed")
                                res = false
                            }
                        } catch (e: IOException) {
                            e(TAG, "TestConnection failed", e)
                            res = false
                        }
                        val hostname = getHostName(context)
                        d(TAG, "Hostname <$hostname>")
                        if (hostname == null || hostname.isEmpty()) {
                            res = false
                        }
                        newCirciut()
                        connectorCallback.onConnected(res)
                        connected = res
                    } catch (e: Exception) {
                        e(TAG, "Error while connect TOR", e)
                    }
                }
                establishServiceConnection(context)
            }

            override fun onReceive(type: ReceiveDataType, data: String?) {
                mOnReceiveClientDataListener.onReceive(type, data)
            }

            override fun onFail(error: Exception?) {
                e(TAG, "Error while input server", error!!)
                connectorCallback.onConnected(false)
            }
        }, serverSettings)
        return false
    }

    override fun isConnected(): Boolean {
        return connected
    }

    companion object {
        const val TAG = "TorConnector"
        @Throws(IOException::class)
        private fun checkIsTor(connection: URLConnection): Boolean {
            var isTor = false
            d("NetCipher", "content length: " + connection.contentLength)
            val jsonReader = JsonReader(InputStreamReader(connection.getInputStream()))
            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                val name = jsonReader.nextName()
                if ("IsTor" == name) {
                    isTor = jsonReader.nextBoolean()
                    break
                }
                jsonReader.skipValue()
            }
            return isTor
        }
    }
}