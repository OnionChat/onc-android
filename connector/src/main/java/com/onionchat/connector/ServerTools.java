package com.onionchat.connector;

import android.util.Log;

import com.onionchat.common.Logging;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerTools {
    public static String TAG = "ServerTools";

    public static boolean isServerSocketInUse(int port) {
        Logging.d(TAG, "isServerSocketInUse: " + port);
        try {
            (new ServerSocket(port)).close();
            return false;
        } catch (IOException e) {
            // Could not connect.
            return true;
        }
    }
}
