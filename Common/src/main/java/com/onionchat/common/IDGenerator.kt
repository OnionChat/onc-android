package com.onionchat.common

import android.util.Base64


object IDGenerator {

    fun toHashedId(uid: String): String {
        return Base64.encodeToString(Crypto.hash(uid.toByteArray()), Base64.DEFAULT)
    }

}