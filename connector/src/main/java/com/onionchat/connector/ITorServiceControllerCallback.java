package com.onionchat.connector;

import androidx.annotation.Nullable;

import com.onionchat.connector.tor.TorNetworkConnectivityStatus;
import com.onionchat.connector.tor.TorServiceInfo;
import com.onionchat.connector.tor.TorServiceConnectionState;

public interface ITorServiceControllerCallback {
    void onTorNetworkConnectivityStatusChanged(TorNetworkConnectivityStatus torNetworkConnectivitiyStatus);
    void onTorServiceConnectionChanged(TorServiceConnectionState serviceConnectionState, @Nullable TorServiceInfo connectionInfo);
}
