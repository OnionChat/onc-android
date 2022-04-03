package com.onionchat.common

import android.util.Log

object Logging {

    @JvmStatic
    fun v(tag: String, message: String) {
        if(BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }

    @JvmStatic
    fun get(): Logging {
        return this
    }

    @JvmStatic
    fun d(tag: String, message: String) {
        if(BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
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