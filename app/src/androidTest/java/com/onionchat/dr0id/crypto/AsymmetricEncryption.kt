package com.onionchat.dr0id.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onionchat.common.Crypto
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.Key
import java.security.cert.Certificate
import javax.crypto.Cipher


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AsymmetricEncryption {
    @Test
    fun encryptWithMyCert() {
        // Context of the app under test.
        val cert: Certificate? = Crypto.getMyPublicKey()
        assertNotNull(cert)

        val cipher: Cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, cert!!.getPublicKey())
        val inputData = byteArrayOf(0, 1, 32, 43, 12)
        val encryptedBytes = cipher.doFinal(inputData)
        assertTrue(encryptedBytes.size > 0)
    }

    @Test
    fun encryptAndDecrypt() {
        // Context of the app under test.
        val cert: Certificate? = Crypto.getMyPublicKey()
        assertNotNull(cert)

        val cipher: Cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, cert!!.getPublicKey())
        val inputData = byteArrayOf(
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
            0, 1, 32, 43,
            12,12,43,34,
        )
        val encryptedBytes = cipher.doFinal(inputData)
        assertTrue(encryptedBytes.size > 0)

        val key: Key = Crypto.getMyKey()
        val dCipher: Cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        dCipher.init(Cipher.DECRYPT_MODE, key)
        val decryptedByes = dCipher.doFinal(encryptedBytes)
        assertEquals(decryptedByes[0], inputData[0])
    }
}