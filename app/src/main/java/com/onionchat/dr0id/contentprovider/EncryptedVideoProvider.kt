package com.onionchat.dr0id.contentprovider


import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.MemoryFile
import android.os.ParcelFileDescriptor
import com.onionchat.common.Logging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.Executors



//sealed class MemoryContentProviderResult {
//    data class Success(val result: ByteArray) : MemoryContentProviderResult()
//    data class Failure(val reason: Exception) : MemoryContentProviderResult()
//}

class EncryptedVideoProvider() : ContentProvider() {

//    private val pool by lazy { Executors.newCachedThreadPool() }

    companion object {
//        var memoryContentProviderCallback: MemoryContentProviderResultCallback<MemoryContentProviderResult>? =
//            null
        var videoBytes :ByteArray? = null

        const val TAG = "EncryptedVideoProvider"
    }


    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        return null
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? {
        return null
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun update(
        p0: Uri,
        p1: ContentValues?,
        p2: String?,
        p3: Array<out String>?
    ): Int {
        return 0
    }

    override fun delete(
        p0: Uri,
        p1: String?,
        p2: Array<out String>?
    ): Int {
        return 0
    }

    override fun getType(p0: Uri): String? {
        return "content"
    }



    override fun openAssetFile(
        uri: Uri,
        mode: String
    ): AssetFileDescriptor? { // please don't implemeent same code twice... This was just for the POC


        /**
         * Here we create the pipe. Please consider to manage the pipe lifecycle.
         * For one use-case instance we should use one pipe.
         */
        val pipe = ParcelFileDescriptor.createReliableSocketPair()

        Thread { // Please think about a nice
            // read the picture
            if(videoBytes == null) {
                Logging.e(TAG, "openAssetFile [-] unable to open video")
                return@Thread
            }
            /**
             * One difference between files and pipe is, that we don't get the "file" size.
             * We have to ensure that we don't get OutOfMemory exceptions. Imagine a malicious app
             * writes a 1gb file into the pipe oO. In this example I used Kotlin's readBytes extension.
             * As I know it brings a maximum of bytes to read and should not cause a OutOfMemory. Please
             * verify this within documentation or a test.
             */
            val outputStream =
                AssetFileDescriptor(
                    pipe[1],
                    0,
                    videoBytes!!.size.toLong() // because we work with a pipe and not with a file it's mandatory to return -1 here.
                ).createOutputStream()
            outputStream.write(videoBytes)

            //memoryContentProviderCallback?.onResult(MemoryContentProviderResult.Success(bytes))
        }.start()

        return AssetFileDescriptor( // let the camera write data into the pipe
            pipe[0]
            , 0, videoBytes!!.size.toLong() // because we work with a pipe and not with a file it's mandatory to return -1 here.
        )
    }
}
