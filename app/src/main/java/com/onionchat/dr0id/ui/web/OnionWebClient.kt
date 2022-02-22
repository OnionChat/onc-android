package com.onionchat.dr0id.ui.web

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.onionchat.common.Logging
import java.io.*
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.util.*


internal class OnionWebClient : WebViewClient() {
    private var requestCounter = 0

    internal interface RequestCounterListener {
        fun countChanged(requestCount: Int)
    }

    @Volatile
    private var requestCounterListener: RequestCounterListener? = null
    fun setRequestCounterListener(requestCounterListener: RequestCounterListener?) {
        this.requestCounterListener = requestCounterListener
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        requestCounter++
        if (requestCounterListener != null) {
            requestCounterListener!!.countChanged(requestCounter)
        }
        val urlString = request.url.toString().split("#").toTypedArray()[0]
        try {
            val connection: HttpURLConnection
            val proxied = true
            connection = if (proxied) {
                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("localhost", 9050))
                URL(urlString).openConnection(proxy) as HttpURLConnection
            } else {
                URL(urlString).openConnection() as HttpURLConnection
            }
            connection.requestMethod = request.method
            for ((key, value) in request.requestHeaders) {
                connection.setRequestProperty(key, value)
            }

            // transform response to required format for WebResourceResponse parameters
            val `in`: InputStream = BufferedInputStream(connection.inputStream)
            val encoding = connection.contentEncoding
            connection.headerFields
            val responseHeaders: MutableMap<String, String> = HashMap()
            for (key in connection.headerFields.keys) {
                key?.let {
                    responseHeaders[it] = connection.getHeaderField(it)
                }
            }
            var mimeType = "text/html"
            if (connection.contentType != null && !connection.contentType.isEmpty()) {
                mimeType = connection.contentType.split("; ").toTypedArray()[0]
            }
            if(mimeType.equals("text/html;charset=utf-8")) {
                mimeType = "text/html"
            }
            Logging.d("OnionWebClient", "opening <" + urlString + "> with mimetype <" + mimeType + ">")
            return WebResourceResponse(mimeType, encoding, connection.responseCode, connection.responseMessage, responseHeaders, `in`)
            //return new WebResourceResponse(mimeType, "binary", in);
        } catch (e: UnsupportedEncodingException) {
        } catch (e: IOException) {
        } catch (e: Exception) {

        }
        // failed doing proxied http request: return empty response
        return WebResourceResponse("text/plain", "UTF-8", 204, "No Content", HashMap(), ByteArrayInputStream(byteArrayOf()))
    }

}