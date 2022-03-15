package com.onionchat.dr0id.stream

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.onionchat.common.IDGenerator
import com.onionchat.common.Logging
import com.onionchat.connector.http.OnionClient
import com.onionchat.connector.tor.OnStreamRequestedListener
import com.onionchat.connector.tor.TorConnector
import com.onionchat.dr0id.R
import io.ktor.utils.io.*
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*


class StreamingWindow : AppCompatActivity() {


    companion object {
        const val TAG = "StreamingWindow"

        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"

        const val CAMERA_PERMISSION_CODE = 101
    }

    val streamController = StreamController(this)

    var statusText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streaming_window)


        // startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        // SYSTEM_ALERT_WINDOW
        intent.getStringExtra(EXTRA_CONVERSATION_ID)?.let { partnerId ->
            findViewById<TextView>(R.id.stream_activity_username)?.let {
                it.text = IDGenerator.toHashedId(partnerId) // retreive user ?
            }

            statusText = findViewById(R.id.stream_activity_status)

            findViewById<ImageButton>(R.id.start_button)?.let { button ->
                button.setOnClickListener {
                    if (streamController.status) {
                        button.setImageDrawable(getDrawable(R.drawable.baseline_call_black_48))
                        button.setBackgroundColor(getColor(android.R.color.holo_green_light))
                        streamController.stop()
                        connected = false
                        //playRecFile()
                    } else {
                        button.setImageDrawable(getDrawable(R.drawable.baseline_call_end_black_48))
                        button.setBackgroundColor(getColor(android.R.color.holo_red_light))
                        openStream(partnerId)
                    }
                }
            }
//            Logging.d(TAG, "start audio stream $partnerId")
//            streamController.startAudioStream { data, size ->
//                Logging.d(TAG, "Insert into buffer ${data.size}")
//                buff.write(data, 0, size) // todo write into connection
//                true
//            }

            listenForStreams()
            openStream(partnerId)
        }


        checkPermissions()
    }

    var streaming = false
    var connected = false
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

    fun listenForStreams() {
        TorConnector.streamListener = object : OnStreamRequestedListener {
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

                val buff = ByteArray(1024)
                streamController.playAudioStream()?.let {
                    Logging.d(TAG, "Read from inputStream ${buff.size}")
                    try {
                        var read = 0
                        do {
                            read = inputStream.read(buff)
                            it?.write(buff, 0, read)
//                        Logging.d(TAG, "wrote into audio $read")
                        } while (read > 0)
                    } catch (exception: Exception) {
                        Logging.e(TAG, "onStreamRequested [+] error while processing stream", exception)
                        connected = false
                    }
                    connected
                }
                return true
            }

        }
    }

    fun openStream(onion_url: String) {
        Logging.d(TAG, "openStream $onion_url")
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
                Logging.d(TAG, "Finished streaming")
            } catch (exception: Exception) {
                // !!!!!!!!!!!!!!! todo improve error handling
                Logging.e(TAG, "Error while open stream", exception)
            }
        }.start()
    }

    fun streamAudio() {
        streaming = true
        Thread {
            streamController.startAudioStream { data, size ->
                try {
//                Logging.d(TAG, "Read from audio ${size}")
                    out!!.write(data, 0, size)
                    //out.flush()
//                Logging.d(TAG, "wrote into outputstream $size")

                } catch (exception: Exception) {
                    Logging.e(TAG, "openStream [+] error while processing stream", exception)
                    false
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


}