package com.onionchat.dr0id.media.stream

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.*
import android.media.AudioManager.STREAM_VOICE_CALL
import android.media.AudioRecord.READ_BLOCKING
import androidx.core.app.ActivityCompat
import com.onionchat.common.Logging
import com.onionchat.dr0id.ui.stream.StreamingWindow


class StreamController() { // todo make this a service or put it into our existing one

    private val sampleRate = 16100
    private val inConfig: Int = AudioFormat.CHANNEL_IN_MONO
    private val outConfig: Int = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_8BIT
    var buffsize = AudioRecord.getMinBufferSize(sampleRate, inConfig, audioFormat)
    var audioRecorder: AudioRecord? = null
    var audioTrack: AudioTrack? = null
    var status = false

    fun startAudioStreamAsync(consumer: (ByteArray, Int) -> Boolean): Boolean {
        Thread() {
            if(!startAudioStream(consumer)) {
                consumer(byteArrayOf(), -1)
            }
        }.start()
        return true
    }

    @SuppressLint("MissingPermission") // will be asked in the UI
    fun startAudioStream(consumer: (ByteArray, Int) -> Boolean): Boolean { // todo don't forget DatagramPacket
        status = true
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return false
//        }
        val buffer = ByteArray(buffsize)
        audioRecorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, inConfig, audioFormat, buffsize)
        //Thread {
        audioRecorder?.let {
            it.startRecording()

            while (status) {


                //reading data from MIC into buffer
                val read = it.read(buffer, 0, buffer.size, READ_BLOCKING)
                if (!consumer(buffer, read)) {
                    status = false
                }
            }
            it.stop()
            status = false
        } ?: run {
            Logging.e(TAG, "startAudioStream [-] unable to retrieve AudioRecord object... it's null... :(")
            return false
        }
        //}.start()
        return audioRecorder != null
    }

    fun playAudioStream(streamType: Int = STREAM_VOICE_CALL): AudioTrack? {

//        val attributes = AudioAttributes.Builder().setLegacyStreamType(STREAM_NOTIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION)
//            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
//        val audioFormat = AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(audioFormat).build()
        //todo revisit
        audioTrack = AudioTrack(
            streamType, sampleRate,
            outConfig,
//            attributes, audioFormat,
            audioFormat,
            buffsize,
            AudioTrack.MODE_STREAM,1001 //todo check session !?
        )
        audioTrack?.let {track->
            if (track.state == AudioTrack.STATE_UNINITIALIZED) {
                Logging.e(StreamingWindow.TAG, "===== AudioTrack Uninitialized =====")
                return null
            }
            track.play()
        }



//        Thread {
//            val arr = callback()
//            track.write(arr, 0, arr.size);
//        }.start()
        return audioTrack
    }

    fun stop() {
        status = false
        audioTrack?.stop()
    }


    companion object {
        const val TAG = "StreamController"
    }
}