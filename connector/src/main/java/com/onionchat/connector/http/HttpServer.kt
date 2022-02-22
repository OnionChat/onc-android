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

//import com.sun.net.httpserver.HttpServer

class HttpServerSettings(val enable_web: Boolean)

class HttpServer(val httpServerCallback: HttpServerCallback, val usedPort: Int, settings: HttpServerSettings) {

    enum class ReceiveDataType {
        POSTMESSAGE,
        PING
    }

    interface HttpServerCallback {
        fun onBound(port: Int)
        fun onReceive(type: ReceiveDataType, data: String?)
        fun onFail(error: Exception?)
        fun onDownloadApk(): String
        fun getWebFolder(): String
    }

    companion object {
        @JvmStatic
        var startPort = 23001

        @JvmStatic
        val TAG = "HttpServer"

        var server: HttpServer? = null

        @JvmStatic
        fun startService(httpServerCallback: HttpServerCallback, settings: HttpServerSettings): Boolean {
            Logging.d(TAG, "Start new server")
            if (server == null) {
                while (ServerTools.isServerSocketInUse(startPort)) {
                    startPort++
                }
                server = HttpServer(httpServerCallback, startPort, settings)
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
                post("/postmessage") {
                    val body = call.receiveText()
                    val agent = call.request.headers["User-Agent"]
                    Logging.d("HttpServer", "Received postmessage agent <" + agent + ">")
                    agent?.let {
                        if (!it.isEmpty()) {
                            Logging.e("HttpServier", "Expecting empty User-Agent field. Abort.")
                        } else {
                            try {
                                httpServerCallback.onReceive(ReceiveDataType.POSTMESSAGE, body)
                            } catch (e: Exception) {
                                Logging.e("HttpServier", "Error while process message. abort", e)
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
                post("/pingmessage") {
                    Logging.d("HttpServer", "Received ping message")
                    val body = call.receiveText()
                    val agent = call.request.headers["User-Agent"]
                    Logging.d("HttpServer", "Received ping agent <" + agent + ">")
                    agent?.let {
                        if (!it.isEmpty()) {
                            Logging.e("HttpServier", "Expecting empty User-Agent field. Abort.")
                        } else {
                            httpServerCallback.onReceive(ReceiveDataType.PING, body)
                        }
                    }
                    Logging.d("HttpServer", "Received ping body <" + body + ">")
                    call.respondText(
                        text = "ok",
                        contentType = ContentType.Text.Plain
                    )
                }
                if (settings.enable_web) {
                    static("/web") {
                        Logging.d("HttpServer", "Setup web access to folder <" + webFolder.absolutePath + ">")
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