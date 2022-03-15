package com.onionchat.connector.http

import com.onionchat.common.Logging
import info.guardianproject.netcipher.NetCipher
import java.io.*
import java.net.HttpURLConnection
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

object OnionClient {

    const val TAG = "OnionClient"

    private val clientExecutor = Executors.newCachedThreadPool()

    private fun post(messageData: String, url: String): String {
        val conn = NetCipher.getHttpURLConnection(url)
        conn.setRequestProperty("User-Agent", "")
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "POST"
        //conn.setDoInput(true);
        //conn.setDoOutput(true);
        Logging.d(TAG, "post [+] Write data")
        val writer = BufferedWriter(OutputStreamWriter(conn.outputStream))
        writer.write(
            messageData
        )
        writer.close()
        Logging.d(TAG, "post [+] Read data")
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        var line: String? = "" // reads a line of text
        val out = StringBuilder()
        while (reader.readLine().also { line = it } != null) {
            out.append(line)
        }
        reader.close()
        Logging.d(TAG, "post [+] Message send done")
        conn.disconnect()
        return out.toString()
    }

    fun postmessage(json: String, onc_addr: String): Future<MessageSentResult> {
        Logging.d(TAG, "postmessage [+]  <$json> to destination <$onc_addr>")
        return clientExecutor.submit(Callable {
            try {
                val url = "http://$onc_addr/postmessage"
                Logging.d(TAG, "postmessage [+] perform <$url>")
                post(json, url).let {
                    Logging.d(TAG, "postmessage [+] received $it")
                }
                MessageSentResult.SENT
            } catch (e: Exception) {
                Logging.e(TAG, "postmessage [-] Error while send message", e)
                MessageSentResult.FAILURE
            }
        })
    }

    fun ping(myID: String, onc_addr: String): Future<Boolean> {
        Logging.d(TAG, "ping [+] <$myID> to destination <$onc_addr>")
        return clientExecutor.submit(Callable<Boolean> {
            try {
                val url = "http://$onc_addr/ping"
                Logging.d(TAG, "postmessage [+] perform <$url>")
                post(myID, url).let {
                    Logging.d(TAG, "postmessage [+] received $it")
                }
                true
            } catch (e: IllegalStateException) {
                Logging.e(TAG, "Error while send message", e);
                true;
            } catch (e: FileNotFoundException) {
                Logging.e(TAG, "Error while send message", e);
                true;
            } catch (e: Exception) {
                Logging.e(TAG, "postmessage [-] Error while send message", e)
                false
            }
        })
    }

    fun openConnection(urlString: String): HttpURLConnection? {
        val connection = NetCipher.getHttpURLConnection(urlString)
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
        connection.setRequestProperty("Accept", "*/*");
        return connection
    }

    fun openStream(onionUrl: String) : HttpURLConnection {
        val conn = NetCipher.getHttpURLConnection(onionUrl)
        conn.setRequestProperty("User-Agent", "")
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.readTimeout = 10000
        conn.connectTimeout = 60 * 60 * 1000 // 1h stream ;)
        conn.requestMethod = "POST"
        conn.setDoInput(true);
        conn.setDoOutput(true);
        return conn
    }

    enum class MessageSentResult {
        SENT,
        FAILURE, // todo timeout error for offline ?
        ONGIONG
    }
}