package com.onionchat.localstorage

import android.content.Context
import java.io.File

object PathProvider {

    @JvmStatic
    fun getAttachmentPath(context: Context): File {
        val f = File(context.getExternalFilesDir(null).toString() + "/attachment/")
        f.mkdirs()
        return f
    }

    @JvmStatic
    fun getWebDir(context: Context): File {
        val f = File(context.getExternalFilesDir(null).toString() + "/web/")
        f.mkdirs()
        return f
    }

    @JvmStatic
    fun getApkPath(context: Context): File {
        return File(context.applicationInfo.sourceDir)
    }


}