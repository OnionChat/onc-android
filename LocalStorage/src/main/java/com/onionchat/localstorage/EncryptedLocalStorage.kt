package com.onionchat.localstorage

import android.content.Context
import android.util.Base64
import com.onionchat.common.Crypto
import java.security.Key
import java.security.cert.Certificate
import javax.crypto.Cipher


class EncryptedLocalStorage(val cert: Certificate, val cryptoKey: Key, val context: Context) {

    val STORAGE_NAME = "EncryptedLocalStorage"

    fun storeValue(key: String, value: String): Boolean {

        val cipher: Cipher = Cipher.getInstance(Crypto.ASYMMETRIC_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, cert)
        val encryptedBytes = cipher.doFinal(value.toByteArray())
        val res = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)

        context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)?.edit()?.let {
            it.putString(key, res)
            it.commit()
        } ?: kotlin.run {
            return false
        }
        return true
    }

    fun getValue(key: String): String? {

        context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)?.getString(key, null)?.let {
            val encrypted_bytes = Base64.decode(it, Base64.DEFAULT)
            val cipher: Cipher = Cipher.getInstance(Crypto.ASYMMETRIC_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, cryptoKey)
            val decryptedBytes = cipher.doFinal(encrypted_bytes)
            return String(decryptedBytes)
        }
        return null
    }


}