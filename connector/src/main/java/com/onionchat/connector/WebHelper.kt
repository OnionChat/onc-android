package com.onionchat.connector

import android.content.Context
import java.io.File
import android.content.res.AssetManager
import com.onionchat.common.Logging
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception


object WebHelper {

    @JvmStatic
    fun getWebDir(context: Context) : File {
        return File(context.getExternalFilesDir(null).toString() + "/web/")
    }


    private fun copyFile(filename: String, destination: File, context: Context) {
        val assetManager: AssetManager = context.getAssets()
        var `in`: InputStream? = null
        var out: OutputStream? = null
        try {
            `in` = assetManager.open(filename)
            val newFileName = destination.absoluteFile
            out = FileOutputStream(newFileName)
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()
            `in` = null
            out.flush()
            out.close()
            out = null
        } catch (e: Exception) {
            Logging.e("WebHelper", "Unable to extract assets",e)
        }
    }

    fun extractDemo(context: Context) {
        val webDir = getWebDir(context)
        if(!webDir.exists()){
            webDir.mkdirs()
            copyFile("webDemo/index.html", File(webDir.absolutePath+"/index.html"), context)
            copyFile("webDemo/onion.png", File(webDir.absolutePath+"/onion.png"), context)
        }
    }
}