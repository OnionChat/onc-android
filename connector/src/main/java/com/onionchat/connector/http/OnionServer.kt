package com.onionchat.connector.http

import com.onionchat.common.Logging
import com.onionchat.connector.OnReceiveClientDataListener
import com.onionchat.connector.ServerTools
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
//import io.ktor.server.jetty.*
import io.ktor.server.cio.*
import java.io.File
import java.io.InputStream

//import com.sun.net.httpserver.HttpServer

class OnionServerSettings(val enable_web: Boolean, val attachmentPath: File, val webPath: File, val apkPath:File)

data class OnionServerInformation(val port:Int)

class OnionServer(private val mOnReceiveClientDataListener: OnReceiveClientDataListener, val usedPort: Int, val settings: OnionServerSettings) {

    enum class ReceiveDataType {
        POSTMESSAGE,
        PING,
        REQUESTPUB,
        RESPONSEPUB,
        SYMKEY
    }



    companion object {

        const val WEB_URL_ATTACHMENT = "/attachment/"

        @JvmStatic
        var startPort = 23001

        @JvmStatic
        val TAG = "OnionServer"

        var server: OnionServer? = null

        @JvmStatic
        fun startOnionServer(onReceiveClientDataListener: OnReceiveClientDataListener, settings: OnionServerSettings): OnionServerInformation {
            Logging.d(TAG, "Start new server")
            if (server == null) {
                while (ServerTools.isServerSocketInUse(startPort)) {
                    startPort = ServerTools.getRandomPort(startPort)
                }
                server = OnionServer(onReceiveClientDataListener, startPort, settings)
                server?.server?.start(false)
            }
            Logging.d(TAG, "Start new server done <${startPort}>")
            return OnionServerInformation(startPort)
        }
    }


    private val server by lazy {
        embeddedServer(CIO, usedPort, watchPaths = emptyList(), host = "127.0.0.1") {
            //install(WebSockets)
            install(CallLogging)
            val webFolder = settings.webPath
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
                                mOnReceiveClientDataListener.onReceive(ReceiveDataType.REQUESTPUB, body)
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
                                mOnReceiveClientDataListener.onReceive(ReceiveDataType.RESPONSEPUB, body)
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
                        mOnReceiveClientDataListener.onStreamRequested(stream)
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
                                mOnReceiveClientDataListener.onReceive(ReceiveDataType.SYMKEY, body)
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
                                mOnReceiveClientDataListener.onReceive(ReceiveDataType.POSTMESSAGE, body)
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
                    mOnReceiveClientDataListener.onDownloadApk()
                    call.respondFile(
                        file = settings.apkPath
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
                            mOnReceiveClientDataListener.onReceive(ReceiveDataType.PING, body)
                        }
                    }
                    Logging.d(TAG, "Received ping body <$body>")
                    call.respondText(
                        text = "ok",
                        contentType = ContentType.Text.Plain
                    )
                }
                static(WEB_URL_ATTACHMENT) { // todo make those strings a constant
                    Logging.d(TAG, "Setup attachment access to folder <" + webFolder.absolutePath + ">")
                    file(".")
                    files(settings.attachmentPath)
                    resources(settings.attachmentPath.absolutePath)
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