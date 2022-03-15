package com.onionchat.connector;

import com.onionchat.connector.http.OnionServer;

public interface OnReceiveClientDataListener {
    void onReceive(OnionServer.ReceiveDataType type, String data);
}
