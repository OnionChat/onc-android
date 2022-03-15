package com.onionchat.dr0id.messaging

import android.util.Base64
import com.onionchat.common.Crypto
import com.onionchat.localstorage.messagestore.EncryptedMessage
import java.security.Key
import java.security.cert.Certificate

class AsymetricMessage(
    val messageId: String = java.util.UUID.randomUUID().toString(),
    var data: ByteArray,
    var hashedFrom: String,
    var hashedTo: String,
    var signature: String = "",
    var messageStatus: Int = 0,
    var created: Long = System.currentTimeMillis(),
    var read: Int = 0,
    var type: Int,
    var extra: String = ""
) {

    fun encrypt(targetPub: Certificate, myPrivate: Key): EncryptedMessage? {
        val encryptedData = Crypto.encryptAsym(targetPub, data)
        Crypto.sign(myPrivate, encryptedData)?.let {
            signature = Base64.encodeToString(it, Base64.DEFAULT)
            return EncryptedMessage(messageId,encryptedData, hashedFrom, hashedTo, signature, messageStatus, created,read,type, extra)
        } ?: run {
            return null
        }
    }


        companion object {
            fun fromEncryptedMessage() {

            }
        }
    }