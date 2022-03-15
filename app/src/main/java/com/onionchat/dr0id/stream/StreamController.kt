package com.onionchat.dr0id.stream

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import androidx.core.app.ActivityCompat
import com.onionchat.common.Logging


class StreamController(val context: Context) { // todo make this a service or put it into our existing one

    private val sampleRate = 44100
    private val inConfig: Int = AudioFormat.CHANNEL_IN_MONO
    private val outConfig: Int = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    var buffsize = AudioRecord.getMinBufferSize(sampleRate, inConfig, audioFormat)
    var audioRecorder: AudioRecord? = null
    var status = false


    fun startAudioStream(consumer: (ByteArray, Int) -> Boolean): Boolean { // todo don't forget DatagramPacket
        status = true
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false
        }
        val buffer = ByteArray(buffsize)
        audioRecorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, inConfig, audioFormat, buffsize)

        //Thread {
        audioRecorder?.let {
            it.startRecording()

            while (status) {


                //reading data from MIC into buffer
                val read = it.read(buffer, 0, buffer.size)
                if (!consumer(buffer, read)) {
                    status = false
                }
            }

        } ?: run {
            return false
        }
        //}.start()
        return audioRecorder != null
    }

    fun playAudioStream(): AudioTrack? {
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC, sampleRate,
            outConfig,
            audioFormat, buffsize,
            AudioTrack.MODE_STREAM
        )

        if (track.getState() === AudioTrack.STATE_UNINITIALIZED) {
            Logging.e(StreamingWindow.TAG, "===== AudioTrack Uninitialized =====")
            return null
        }
        track.play()
//        Thread {
//            val arr = callback()
//            track.write(arr, 0, arr.size);
//        }.start()
        return track
    }

    fun stop() {
        status = false
    }
}