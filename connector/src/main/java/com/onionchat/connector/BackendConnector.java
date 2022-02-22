package com.onionchat.connector;


import com.onionchat.common.Logging;
import com.onionchat.connector.tor.TorConnector;

import java.util.HashSet;

public class BackendConnector {

    private static IConnector connector;
    private static HashSet<OnReceiveClientDataListener> listeners = new HashSet<>();

    public static IConnector getConnector() {
        if (connector == null) {
            connector = new TorConnector((type, data) -> {
                Logging.d("BackendConnector", "listeners <" + listeners.size() + ">");
                for (OnReceiveClientDataListener onReceiveClientDataListener : listeners) {
                    onReceiveClientDataListener.onReceive(type, data);
                }
            });
        }
        return connector;
    }

    public static void registerOnReceiveCallback(OnReceiveClientDataListener onReceiveClientDataListener) {
        listeners.add(onReceiveClientDataListener);
    }
}
