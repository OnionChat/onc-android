package com.onionchat.dr0id.ui.contentresolver

import android.content.Context
import android.net.Uri
import com.onionchat.common.Logging
import java.io.InputStream

object ContentResolverHelper {

    const val TAG = "ContentResolverHelper"

    fun readFromUri(context: Context, uri: Uri) : ByteArray? {
        var stream: InputStream? = context.contentResolver.openInputStream(uri)
        if(stream == null) {
            Logging.e(TAG, "readFromUri [+] ")
            return null
        } else {
            return stream.readBytes()
        }
    }

}