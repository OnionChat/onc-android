package com.onionchat.dr0id.messaging

import android.util.Base64
import com.onionchat.common.Crypto
import com.onionchat.common.Logging
import com.onionchat.localstorage.messagestore.EncryptedMessage
import org.json.JSONObject
import java.security.Key
import java.security.cert.Certificate

open class SymmetricMessage(
    messageId: String = java.util.UUID.randomUUID().toString(),
    var data: ByteArray,
    hashedFrom: String,
    hashedTo: String,
    signature: String = "",
    messageStatus: Int = 0,
    created: Long = System.currentTimeMillis(),
    read: Int = 0,
    type: Int,
    extra: String = ""
) : IMessage(messageId, hashedFrom, hashedTo, signature, messageStatus, created, read, type, extra) {


    companion object {

        val TAG = "SymmetricMessage"
        val EXTRA_IV = "iv"
        val EXTRA_ALIAS = "alias"


        fun encrypt(symmetricMessage: SymmetricMessage, alias: String, myPrivate: Key): EncryptedMessage? {
            Logging.d(TAG, "encrypt [-] going to encrypt symmetric message <$symmetricMessage> with alias <$alias>")
            val targetSymmetricKey = Crypto.getSymmetricKey(alias)
            if (targetSymmetricKey == null) {
                Logging.e(TAG, "encrypt [-] unable to retrieve symmetric key <$alias>")
                return null
            }
            val res = Crypto.encryptSym(targetSymmetricKey, symmetricMessage.data)
            val data = res.encryptedData
            val extras = if (symmetricMessage.extra.isEmpty()) JSONObject() else JSONObject(symmetricMessage.extra)
            Logging.d(TAG, "encrypt [+] encryptSym(${targetSymmetricKey.algorithm}, ${res.iv.size}, ${symmetricMessage.data.size}) res ${data.size}")
            extras.put(EXTRA_IV, Base64.encodeToString(res.iv, Base64.DEFAULT))
            extras.put(EXTRA_ALIAS, alias)

            Crypto.sign(myPrivate, data)?.let {
                symmetricMessage.signature = Base64.encodeToString(it, Base64.DEFAULT)
                return EncryptedMessage(
                    symmetricMessage.messageId,
                    data,
                    symmetricMessage.hashedFrom,
                    symmetricMessage.hashedTo,
                    symmetricMessage.signature,
                    symmetricMessage.messageStatus,
                    symmetricMessage.created,
                    symmetricMessage.read,
                    symmetricMessage.type,
                    extras.toString()
                )
            } ?: run {
                Logging.e(TAG, "Unable to sign message")
                return null
            }
        }

        fun decrypt(encryptedMessage: EncryptedMessage, sourcePub: Certificate?): SymmetricMessage? {
            Logging.d(TAG, "decrypt [-] going to decrypt symmetric message <$encryptedMessage>")
            if(sourcePub == null ) {
                Logging.e(TAG, "decrypt [-] sourcePub is null. Cannot validate signature !!! This is an untrusted message !! shall we show it?")
            }
            val extras = JSONObject(encryptedMessage.extra)
            if (!extras.has(EXTRA_ALIAS) || !extras.has(EXTRA_IV)) {
                Logging.e(TAG, "decrypt [-] invalid extras <${encryptedMessage.extra}>")
                return null
            }
            val iv = Base64.decode(extras.getString(EXTRA_IV), Base64.DEFAULT)
            val alias = extras.getString(EXTRA_ALIAS)
            val key = Crypto.getSymmetricKey(alias)
            if (key == null) {
                Logging.e(TAG, "decrypt [-] unable to find key with alias <$alias>")
                throw UnknownKeyException("Unable to find key <$alias>", alias)
            }
            Crypto.decryptSym(key, iv, encryptedMessage.encryptedMessageBytes)?.let { decryptedData ->
                if (sourcePub == null || Crypto.verify(sourcePub, encryptedMessage.encryptedMessageBytes, Base64.decode(encryptedMessage.signature, Base64.DEFAULT))) {
                    return SymmetricMessage(
                        encryptedMessage.messageId,
                        decryptedData,
                        encryptedMessage.hashedFrom,
                        encryptedMessage.hashedTo,
                        encryptedMessage.signature,
                        encryptedMessage.messageStatus,
                        encryptedMessage.created,
                        encryptedMessage.read,
                        encryptedMessage.type,
                        encryptedMessage.extra
                    )
                } else {
                    Logging.e(TAG, "Error while verify message <$encryptedMessage>")
                }
            } ?: run {
                Logging.e(TAG, "Error while decrypt message <$encryptedMessage>")
            }
            return null
        }
    }

    class UnknownKeyException(msg: String, val alias: String) : Exception(msg)

    override fun getMessageType(): Int {
        return type
    }
}