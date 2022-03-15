package com.onionchat.connector.http

import com.onionchat.common.Logging
import com.onionchat.connector.ServerTools
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream

//import com.sun.net.httpserver.HttpServer

class HttpServerSettings(val enable_web: Boolean)

class OnionServer(val httpServerCallback: HttpServerCallback, val usedPort: Int, settings: HttpServerSettings) {

    enum class ReceiveDataType {
        POSTMESSAGE,
        PING,
        REQUESTPUB,
        RESPONSEPUB,
        SYMKEY
    }

    interface HttpServerCallback {
        fun onBound(port: Int)
        fun onReceive(type: ReceiveDataType, data: String?)
        fun onFail(error: Exception?)
        fun onDownloadApk(): String
        fun getWebFolder(): String
        fun onStreamRequested(inputStream: InputStream): Boolean
    }

    companion object {
        @JvmStatic
        var startPort = 23001

        @JvmStatic
        val TAG = "OnionServer"

        var server: OnionServer? = null

        @JvmStatic
        fun startService(httpServerCallback: HttpServerCallback, settings: HttpServerSettings): Boolean {
            Logging.d(TAG, "Start new server")
            if (server == null) {
                while (ServerTools.isServerSocketInUse(startPort)) {
                    startPort++
                }
                server = OnionServer(httpServerCallback, startPort, settings)
                server?.server?.start(false)
            }
            httpServerCallback.onBound(startPort)
            Logging.d(TAG, "Start new server done")
            return true
        }
    }


    private val server by lazy {
        embeddedServer(Jetty, usedPort, watchPaths = emptyList(), host = "127.0.0.1") {
            //install(WebSockets)
            install(CallLogging)
            val webFolder = File(httpServerCallback.getWebFolder())
            routing {
                post("/requestpub") {
                    val body = call.receiveText()
                    val agent = call.request.headers["User-Agent"]
                    Logging.d(TAG, "Received postmessage agent <" + agent + ">")
                    agent?.let {
                        if (!it.isEmpty()) {
                            Logging.e(TAG, "Expecting empty User-Agent field. Abort.")
                        } else {
                            try {
                                httpServerCallback.onReceive(ReceiveDataType.REQUESTPUB, body)
                            } catch (e: Exception) {
                                Logging.e(TAG, "Error while process message. abort", e)
                                throw e
                            }
                        }
                    }
                    call.respondText(
                        text = "ok",
                        contentType = ContentType.Text.Plain
                    )
                }
                post("/responsepub") {
                    val body = call.receiveText()
                    val agent = call.request.headers["User-Agent"]
                    Logging.d(TAG, "Received postmessage agent <" + agent + ">")
                    agent?.let {
                        if (!it.isEmpty()) {
                            Logging.e(TAG, "Expecting empty User-Agent field. Abort.")
                        } else {
                            try {
                                httpServerCallback.onReceive(ReceiveDataType.RESPONSEPUB, body)
                            } catch (e: Exception) {
                                Logging.e(TAG, "Error while process message. abort", e)
                                throw e
                            }
                        }
                    }
                    call.respondText(
                        text = "ok",
                        contentType = ContentType.Text.Plain
                    )
                }
                post("/stream/audio") {
                    //val body = call.receiveText()
                    Logging.d(TAG, "Received stream request")
                    call.receive<InputStream>().use { stream ->
                        httpServerCallback.onStreamRequested(stream)
                    }

//                    call.respondOutputStream(ContentType.parse("application/octet-stream"), status = HttpStatusCode.OK) {
//                        /** ContentType.parse("application/octet-stream") **/
//
//                        httpServerCallback.onStreamRequested(body, this)
//                    }
                }
                post("/symkey") {
                    val body = call.receiveText()
                    val agent = call.request.headers["User-Agent"]
                    Logging.d("HttpServer", "Received postmessage agent <" + agent + ">")
                    agent?.let {
                        if (!it.isEmpty()) {
                            Logging.e(TAG, "Expecting empty User-Agent field. Abort.")
                        } else {
                            try {
                                httpServerCallback.onReceive(ReceiveDataType.SYMKEY, body)
                            } catch (e: Exception) {
                                Logging.e(TAG, "Error while process message. abort", e)
                                throw e
                            }
                        }
                    }
                    call.respondText(
                        text = "ok",
                        contentType = ContentType.Text.Plain
                    )
                }
                post("/postmessage") {
                    val body = call.receiveText()
                    val agent = call.request.headers["User-Agent"]
                    Logging.d(TAG, "Received postmessage agent <" + agent + ">")
                    agent?.let {
                        if (!it.isEmpty()) {
                            Logging.e(TAG, "Expecting empty User-Agent field. Abort.")
                        } else {
                            try {
                                httpServerCallback.onReceive(ReceiveDataType.POSTMESSAGE, body)
                            } catch (e: Exception) {
                                Logging.e(TAG, "Error while process message. abort", e)
                                throw e
                            }
                        }
                    }
                    call.respondText(
                        text = "ok",
                        contentType = ContentType.Text.Plain
                    )
                }
                get("/onionchat.apk") {
                    call.respondFile(
                        file = File(httpServerCallback.onDownloadApk())
                    )
                }
                post("/ping") {
                    Logging.d("HttpServer", "Received ping message")
                    val body = call.receiveText()
                    val agent = call.request.headers["User-Agent"]
                    Logging.d(TAG, "Received ping agent <" + agent + ">")
                    agent?.let {
                        if (!it.isEmpty()) {
                            Logging.e(TAG, "Expecting empty User-Agent field. Abort.")
                        } else {
                            httpServerCallback.onReceive(ReceiveDataType.PING, body)
                        }
                    }
                    Logging.d(TAG, "Received ping body <" + body + ">")
                    call.respondText(
                        text = "ok",
                        contentType = ContentType.Text.Plain
                    )
                }
                if (settings.enable_web) {
                    static("/web") {
                        Logging.d(TAG, "Setup web access to folder <" + webFolder.absolutePath + ">")
                        //staticBasePackage = webFolder.absolutePath
                        file(".")
                        files(webFolder.absolutePath)
                        resources(webFolder.absolutePath)
                        //files(webFolder.absolutePath)
                        //staticRootFolder = webFolder
                        defaultResource("index.html")
                    }
                }
            }
        }
    }
}