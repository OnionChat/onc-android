package com.onionchat.dr0id.ui.viewer

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.RequestBuilder

class MediaViewerModel : ViewModel() {
    // TODO: Implement the ViewModel
    val image = MutableLiveData<RequestBuilder<Drawable>>()
    val video = MutableLiveData<ByteArray>()
    val error = MutableLiveData<String>()
}