package com.onionchat.connector;

import android.content.Context;

import com.onionchat.connector.http.HttpServerSettings;

import java.io.IOException;

public interface IConnector {
    boolean connect(Context context, IConnectorCallback connectorCallback, HttpServerSettings serverSettings);
    boolean isConnected();
    String getHostName(Context context) throws IOException;
    int getHttpTunnelPort();
    int getSocksPort();
    String newCirciut();
    String getInfo();
    boolean reconnect(Context context);
}
