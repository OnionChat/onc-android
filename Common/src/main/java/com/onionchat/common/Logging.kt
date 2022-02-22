package com.onionchat.common

import android.util.Log

object Logging {


    @JvmStatic
    fun get(): Logging {
        return this
    }

    @JvmStatic
    fun d(tag: String, message: String) {
        Log.d(tag, message);
    }

    @JvmStatic
    fun e(tag: String, message: String) {
        Log.e(tag, message);
    }

    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable);
    }

    fun getInstance(): Logging {
        return Logging;
    }
}