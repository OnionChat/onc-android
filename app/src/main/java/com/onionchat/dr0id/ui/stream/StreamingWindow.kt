package com.onionchat.dr0id.ui.stream

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.connector.http.OnionClient
import com.onionchat.dr0id.OnionChatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.connectivity.ConnectionManager
import com.onionchat.dr0id.connectivity.PingPayload
import com.onionchat.dr0id.connectivity.PingPayload.Companion.PURPOSE_STREAM
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.media.stream.StreamController
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.queue.OnionTask
import com.onionchat.dr0id.queue.tasks.CheckConnectionTask
import io.ktor.utils.io.*
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*


class StreamingWindow : OnionChatActivity() {


    companion object {
        const val TAG = "StreamingWindow"

        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_IS_INCOMMING = "extra_is_incomming"

        const val CAMERA_PERMISSION_CODE = 101
    }


    val streamController = StreamController()

    var statusText: TextView? = null

    var conversation: Conversation? = null

    var button: ImageButton? = null

    var incomming = false

    var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streaming_window)


        // startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        // SYSTEM_ALERT_WINDOW
        incomming = intent.hasExtra(EXTRA_IS_INCOMMING)
        intent.getStringExtra(EXTRA_CONVERSATION_ID)?.let { partnerId ->
            UserManager.getUserById(partnerId).get()?.let { // todo change to then ?
                conversation = Conversation(it)
            }
            findViewById<TextView>(R.id.stream_activity_username)?.let {
                it.text = IDGenerator.toHashedId(partnerId) // retreive user ?
            }

            statusText = findViewById(R.id.stream_activity_status)

            button = findViewById(R.id.start_button)
            button?.let { button ->

                button.setOnClickListener {
                    if (connected && !incomming) {
                        connected = false
                        //playRecFile()
                    } else {
//                        incomming = false // todo dirty hack
                        ringtone?.stop()
                        uiState(true)
                        conversation?.user?.let {
                            startRecursivePing(user = it, purpose = PingPayload(PURPOSE_STREAM))
                        }
                    }
                }
            }
//            Logging.d(TAG, "start audio stream $partnerId")
//            streamController.startAudioStream { data, size ->
//                Logging.d(TAG, "Insert into buffer ${data.size}")
//                buff.write(data, 0, size) // todo write into connection
//                true
//            }

        } ?: run {
            Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_LONG).show()
        }
        if (incomming) {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()
            uiState(false)
        } else {
            uiState(true)
        }

        checkPermissions()
    }

    override fun onConversationOnline(online : Boolean) {
        if(!online) {
            connected = false
            return
        }
        if (incomming) { // todo this is unsafe!
            conversation?.user?.id?.let {
                openStream(it)
                incomming = false
            }
        }
    }

    override fun onCheckConnectionFinished(status: CheckConnectionTask.CheckConnectionResult) {
        if (status.status != OnionTask.Status.SUCCESS) {
            updateConnectionState(ConnectionManager.ConnectionState.ERROR)
            connected = false
        } else {
            updateConnectionState(ConnectionManager.ConnectionState.CONNECTED)
            if (!incomming) {
                conversation?.user?.let {
                    startRecursivePing(user = it, purpose = PingPayload(PURPOSE_STREAM))
                }
            }
        }
    }

    var streaming = false
    var connected = false
        set(value) {
            field = value
            onConnectionStateChanged(value)
        }

    var startTime: Long = 0
    var out: OutputStream? = null
    var conn: HttpURLConnection? = null

    fun startClock() {
        connected = true
        startTime = System.currentTimeMillis()
        Thread {
            while (connected) {
                SystemClock.sleep(1000)
                val diff = System.currentTimeMillis() - startTime;
                val ms_SDF = SimpleDateFormat("mm:ss")
                val elapsed: String = ms_SDF.format(Date(diff))
                runOnUiThread {
                    statusText?.text = elapsed
                }
            }
        }.start()
    }


    fun openStream(onion_url: String) {
        Logging.d(TAG, "openStream $onion_url")
        ringtone?.stop()
        Thread {
            try {
                conn = OnionClient.openStream("http://$onion_url/stream/audio")
                conn!!.setChunkedStreamingMode(streamController.buffsize)
                out = DataOutputStream(conn!!.outputStream)
                conn!!.connect()
                if (connected && !streaming) {
                    streamAudio()
                }
                val headerBytes = ByteArray(streamController.buffsize) // random 512 bytes. later maybe user or crypto info
                SecureRandom().nextBytes(headerBytes)
                out!!.write(headerBytes, 0, streamController.buffsize)
                out!!.flush()
                Logging.d(TAG, "stream opened")
            } catch (exception: Exception) {
                // !!!!!!!!!!!!!!! todo improve error handling
                Logging.e(TAG, "Error while open stream", exception)
                connected = false
            }
        }.start()
    }

    fun streamAudio() {
        streaming = true
        Thread {
            var failures = 0
            streamController.startAudioStream { data, size ->
                try {
//                Logging.d(TAG, "Read from audio ${size}")

                    out!!.write(data, 0, size)
                    //out.flush()
//                Logging.d(TAG, "wrote into outputstream $size")

                } catch (exception: Exception) {
                    Logging.e(TAG, "openStream [+] error while processing stream", exception)
                    failures += 1
                    if (failures > 3)
                        connected = false
                }
                connected
            }
        }.start()
    }

    fun checkPermissions() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), CAMERA_PERMISSION_CODE);
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {

            // Checking whether user granted the permission or not.
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Showing the toast message
                Toast.makeText(this@StreamingWindow, "Camera Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@StreamingWindow, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStreamRequested(inputStream: InputStream): Boolean {
        Logging.d(TAG, "stream requested ")
        startClock()
        if (connected && !streaming) {
            streamAudio()
        }
        // read random bytes

        val header = ByteArray(streamController.buffsize) // random 512 bytes
        val read = inputStream.read(header)
        if (read != header.size) {
            Logging.e(TAG, "onStreamRequested [+] !WARNING! didn't read header <$read> of <${header.size}>")
        }

        val buff = ByteArray(streamController.buffsize)
        streamController.playAudioStream()?.let {
            Logging.d(TAG, "Read from inputStream ${buff.size}")
            try {
                var read = 0
                do {
                    read = inputStream.read(buff)
                    it?.write(buff, 0, read)
//                        Logging.d(TAG, "wrote into audio $read")
                } while (read > 0 && connected)
            } catch (exception: Exception) {
                Logging.e(TAG, "onStreamRequested [+] error while processing stream", exception)
                connected = false
            }
            connected
        }
        return true
    }

    override fun onPingReceived(pingPayload: PingPayload): Boolean {
        // ping received !?
        Logging.d(TAG, "onPingReceived ${pingPayload}")
        if (pingPayload.purpose == PURPOSE_STREAM && pingPayload.uid == conversation?.user?.getHashedId()) {
            conversation?.user?.let {
                if (!incomming) {
                    openStream(it.id)
                }
            }
        }

        return true
    }

    fun onConnectionStateChanged(state: Boolean) {
        if (!state) {
            onStreamCanceled()
        }
    }

    fun onStreamCanceled() {
        uiState(false)
        streamController.stop()
        Thread {
            runOnUiThread {
                button?.isEnabled = false
            }
            SystemClock.sleep(3000)
            runOnUiThread {
                finish()
            }
        }.start()
    }

    fun uiState(accepted: Boolean) {
        runOnUiThread {
            if (accepted) {
                button?.setImageDrawable(getDrawable(R.drawable.baseline_call_end_black_48))
                button?.backgroundTintList = getColorStateList(android.R.color.holo_red_light)
            } else {
                button?.setImageDrawable(getDrawable(R.drawable.baseline_call_black_48))
                button?.backgroundTintList = getColorStateList(android.R.color.holo_green_light)
            }
        }

    }
}