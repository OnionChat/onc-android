package com.onionchat.connector;

import com.onionchat.connector.http.HttpServer;

public interface OnReceiveClientDataListener {
    void onReceive(HttpServer.ReceiveDataType type, String data);
}
