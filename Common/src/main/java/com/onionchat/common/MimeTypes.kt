package com.onionchat.common

object MimeTypes {

    fun isSupportedAudio(mimeType:String) : Boolean {
        return mimeType.contains("audio")
    }

    fun isSupportedImage(mimeType:String) : Boolean {
        return mimeType.contains("image")
    }

    fun isSupportedVideo(mimeType:String) : Boolean {
        return mimeType.contains("video")
    }
}