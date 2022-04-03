package com.onionchat.connector

import com.onionchat.connector.http.OnionServer.ReceiveDataType
import java.io.InputStream

interface OnReceiveClientDataListener {
    fun onDownloadApk()
    fun onReceive(type: ReceiveDataType, data: String)
    fun onStreamRequested(inputStream: InputStream): Boolean // this shall be processed blocking. The connection will be lost after this callback has been returned
}