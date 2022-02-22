package com.onionchat.connector;

import com.onionchat.common.Logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import info.guardianproject.netcipher.NetCipher;

public class Communicator {
    public static final String TAG = "Communicator";

    private static ExecutorService communicationExecutor = Executors.newCachedThreadPool();


    private Communicator() {
        ;
    }

    public enum MessageSentStatus {
        ERROR,
        SENT,
        ONGIONG
    }

    public interface CommunicationCallback {
        void onMessageSent(MessageSentStatus status);
    }


    public static Future<String> sendMessage(String onc_addr, String message) {
        return sendMessage(onc_addr, message, null);
    }

    public static Future<String> sendMessage(String onc_addr, String message, CommunicationCallback callback) {
        Logging.d("Communicator", "sendMessage <" + message + ">");
        return communicationExecutor.submit(() -> {
            try {
                String url = "http://" + onc_addr + "/postmessage";
                Logging.d("Communicator", "persform <" + url + ">");
                HttpURLConnection conn = NetCipher.getHttpURLConnection(url);
                conn.setRequestProperty("User-Agent", "");
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                //conn.setDoInput(true);
                //conn.setDoOutput(true);

                Logging.d("Communicator", "Write data");
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                writer.write("\n" + message + "\n");
                writer.close();

                Logging.d("Communicator", "Read data");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = "";    // reads a line of text
                StringBuilder out = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                }
                reader.close();
                Logging.d("Communicator", "Message send done");
                conn.disconnect();
                callback.onMessageSent(MessageSentStatus.SENT);
            } catch (Exception e) {
                Logging.e("Communicator", "Error while send message", e);
                callback.onMessageSent(MessageSentStatus.ERROR);
            }
            return "";
        });
    }

    public static Future<Boolean> isUserOnline(String onc_addr, String myVisibleId) {
        return communicationExecutor.submit(() -> {
            try {
                String url = "http://" + onc_addr + "/pingmessage";
                Logging.d("Communicator", "persform <" + url + ">");
                HttpURLConnection conn = NetCipher.getHttpURLConnection(url);
                conn.setRequestProperty("User-Agent", "");
//                conn.setDoInput(true);
//                conn.setDoOutput(true);
                //conn.setRequestProperty("User-Agent", "OnionChat <" + BuildConfig.LIBRARY_PACKAGE_NAME + ">");
                //Logging.d("Communicator", "Agent: <"+conn.getHeaderField("User-Agent")+">");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("POST");



                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                writer.write(myVisibleId);
                writer.close();
//                conn.connect();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = "";    // reads a line of text
                StringBuilder out = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                }
                reader.close();
                Logging.d("Communicator", "Ping done <" + line + ">");
                conn.disconnect();
            } catch (IllegalStateException e) {
                Logging.e("Communicator", "Error while send message", e);
                return true;
            }catch (FileNotFoundException e) {
                Logging.e("Communicator", "Error while send message", e);
                return true;
            } catch (Exception e) {
                Logging.e("Communicator", "Error while send message", e);
                return false;
            }
            return true;
        });
    }
}
