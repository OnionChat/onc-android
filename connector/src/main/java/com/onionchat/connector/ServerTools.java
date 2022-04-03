package com.onionchat.connector;

import android.util.Log;

import com.onionchat.common.Logging;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

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

    public static int getRandomPort(int orientation) {
        int randomNum = ThreadLocalRandom.current().nextInt(orientation, orientation+1000);
        return randomNum;
    }
}
