package com.onionchat.dr0id.queue.tasks

import com.onionchat.common.Logging
import com.onionchat.connector.http.OnionClient
import com.onionchat.dr0id.connectivity.ConnectionInformation
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.connectivity.PingPayload
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.OnionTaskProcessor.waitForGlobalTaskResult
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class CheckConnectionTask : OnionTask<CheckConnectionTask.CheckConnectionResult>() {


    class CheckConnectionResult(status: Status, val connectionInformation: ConnectionInformation? = null, exception: Exception? = null) :
        OnionTask.Result(status, exception) {
    }

    override fun onUnhandledException(exception: Exception): CheckConnectionTask.CheckConnectionResult {
        return CheckConnectionTask.CheckConnectionResult(status = Status.FAILURE, exception = exception)
    }

    companion object {
        val TAG = "CheckConnectionTask"
    }

    override fun run(): CheckConnectionResult {
        // call get
        //OnionClient.get()
//        val basicRes = torBasicTest()
//        if (basicRes == null) {
//            Logging.e(TAG, "run [-] !! WARNING !! tor ip test failed.. continue testing")
//        }

        val pingMyselfRes = pingMyselfTest()
        if(!pingMyselfRes) {
            Logging.e(TAG, "run [-] !! ERROR !! unable to ping myself")
            return CheckConnectionResult(Status.FAILURE, connectionInformation = null)
        }

        return CheckConnectionResult(Status.SUCCESS, connectionInformation = null)
    }

    fun pingMyselfTest() : Boolean {
        val myId = UserManager.myId
        if(myId == null) {
            Logging.e(TAG, "pingMyselfTest [-] error while perform ping myselff test... myId is null")
            return false
        }
        val res = enqueueSubtask(PingTask(myId, PingPayload()))
        Logging.d(TAG, "pingMyselfTest [+] waitForGlobalTaskResult")

        var pingResult = waitForGlobalTaskResult(HandlePingTask.HandlePingResult::class.java)
        Logging.d(TAG, "pingMyselfTest [+] pingResult=$pingResult)")
        return (pingResult != null)
    }

    fun torBasicTest(): ConnectionInformation? {
        val commonRes = parseTorIpCheckResult(commonGet(TOR_IP_CHECK_URL))
        Logging.d(TAG, "run [+] got <$commonRes> via common connection")
        if (commonRes == null) {
            Logging.e(TAG, "run [-] unable to perform tor ip test.. result is invalid")
            return null
        }

        if (commonRes.isTor) {
            Logging.d(TAG, "run [+] client seems to use tor as vpn service!?")
        }

        Logging.d(TAG, "run [+] common ip is <${commonRes.ip}>")

        val onionClientRes = parseTorIpCheckResult(OnionClient.get(TOR_IP_CHECK_HTTP_URL))
        if (onionClientRes == null) {
            Logging.e(TAG, "run [-] unable to perform tor ip test.. result is invalid")
            throw IOException("Tor isn't connected dude... !!")
        }

        return ConnectionInformation(commonRes.ip, "")
    }

    data class TorIpCheckResult(val isTor: Boolean, val ip: String)

    val TOR_IP_CHECK_URL = "https://check.torproject.org/api/ip"
    val TOR_IP_CHECK_HTTP_URL = "http://check.torproject.org/api/ip"
    val TOR_IP_CHECK_IS_TOR = "IsTor"
    val TOR_IP_CHECK_IP = "IP"

    fun parseTorIpCheckResult(res: String): TorIpCheckResult? {
        if (res.length <= 0) {
            Logging.e(TAG, "parseTorIpCheckResult [-] invalid response data")
            return null
        }
        val json = JSONObject(res)
        if (!json.has(TOR_IP_CHECK_IS_TOR) ||
            !json.has(TOR_IP_CHECK_IP)
        ) {
            Logging.e(TAG, "parseTorIpCheckResult [-] invalid json <${res}>")
            return null
        }
        return TorIpCheckResult(json.getBoolean(TOR_IP_CHECK_IS_TOR), json.getString(TOR_IP_CHECK_IP))
    }

    fun commonGet(url: String): String { // this doesn't use tor !!
        val url = URL(url)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.setRequestMethod("GET")
        connection.setDoOutput(true)
        connection.setConnectTimeout(5000)
        connection.setReadTimeout(5000)
        connection.connect()
        val rd = BufferedReader(InputStreamReader(connection.getInputStream()))
        var content = ""
        var line: String?
        while (rd.readLine().also { line = it } != null) {
            content += """
                $line
                """
        }
        return content
    }

}