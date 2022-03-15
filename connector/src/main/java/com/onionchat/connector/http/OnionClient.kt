package com.onionchat.connector.http

import com.onionchat.common.Logging.d
import com.onionchat.common.Logging.e
import info.guardianproject.netcipher.NetCipher
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

object HttpClient {

    const val TAG = "HttpClient"

    private val clientExecutor = Executors.newCachedThreadPool()


    fun postmessage(json: String, onc_addr: String): Future<MessageSentResult> {
        d("Communicator", "postmessage [+]  <$json> to destination <$onc_addr>")
        return clientExecutor.submit(Callable {
            try {
                val url = "http://$onc_addr/postmessage"
                d(TAG, "postmessage [+] perform <$url>")
                val conn = NetCipher.getHttpURLConnection(url)
                conn.setRequestProperty("User-Agent", "")
                conn.readTimeout = 10000
                conn.connectTimeout = 15000
                conn.requestMethod = "POST"
                //conn.setDoInput(true);
                //conn.setDoOutput(true);
                d(TAG, "postmessage [+] Write data")
                val writer = BufferedWriter(OutputStreamWriter(conn.outputStream))
                writer.write(
                    json.trimIndent()
                )
                writer.close()
                d(TAG, "postmessage [+] Read data")
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                var line: String? = "" // reads a line of text
                val out = StringBuilder()
                while (reader.readLine().also { line = it } != null) {
                    out.append(line)
                }
                reader.close()
                d(TAG, "postmessage [+] Message send done")
                conn.disconnect()
                MessageSentResult.SENT
            } catch (e: Exception) {
                e(TAG, "postmessage [-] Error while send message", e)
                MessageSentResult.FAILURE
            }
            MessageSentResult.FAILURE
        })
    }

    enum class MessageSentResult {
        SENT,
        FAILURE, // todo timeout error for offline ?
        ONGIONG
    }
}