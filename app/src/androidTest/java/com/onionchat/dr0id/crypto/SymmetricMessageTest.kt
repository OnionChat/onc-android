package com.onionchat.dr0id.crypto

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onionchat.common.Crypto
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.SymmetricMessage
import junit.framework.Assert.*
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStoreException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.SecretKey


@RunWith(AndroidJUnit4::class)
class SymmetricMessageTest {

    val testalias = "testalias"


    @After
    fun tearDown() {
        Crypto.storeSymmetricKey(testalias, null)
    }

    @Before
    fun setUp() {
        Crypto.generateSymmetricKey(testalias)
    }

    @Test
    fun simpleSymmetricMessageEncryption() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val data = byteArrayOf(12, 32, 1, 0, 32, 43, 12)
        val message = SymmetricMessage(data = data, hashedFrom = hashedFrom, hashedTo = hashedTo, type = MessageTypes.TEXT_MESSAGE.ordinal)
        assertNotNull(SymmetricMessage.encrypt(message, testalias, Crypto.getMyKey()))
    }

    @Test
    fun checkEncryptedMessageParams() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val data = byteArrayOf(12, 32, 1, 0, 32, 43, 12)
        val message = SymmetricMessage(data = data, hashedFrom = hashedFrom, hashedTo = hashedTo, type = MessageTypes.TEXT_MESSAGE.ordinal)
        val encryptedMessage = SymmetricMessage.encrypt(message, testalias, Crypto.getMyKey())
        assertNotNull(encryptedMessage)
        assertEquals(encryptedMessage!!.messageId, message.messageId)
        assertEquals(encryptedMessage!!.hashedFrom, message.hashedFrom)
        assertEquals(encryptedMessage!!.hashedTo, message.hashedTo)
        assertEquals(encryptedMessage!!.signature, message.signature)
        assertEquals(encryptedMessage!!.messageStatus, message.messageStatus)
        assertEquals(encryptedMessage!!.created, message.created)
        assertEquals(encryptedMessage!!.read, message.read)
        assertEquals(encryptedMessage!!.type, message.type)
    }

    @Test
    fun simpleSymmetricMessageEncryptionInvalidAlias() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val data = byteArrayOf(12, 32, 1, 0, 32, 43, 12)
        val message = SymmetricMessage(data = data, hashedFrom = hashedFrom, hashedTo = hashedTo, type = MessageTypes.TEXT_MESSAGE.ordinal)
        assertNull(SymmetricMessage.encrypt(message, "invalidalias", Crypto.getMyKey()))
    }

    @Test(expected = ClassCastException::class)
    fun simpleSymmetricMessageEncryptionBrokenKey() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val data = byteArrayOf(12, 32, 1, 0, 32, 43, 12)
        val message = SymmetricMessage(data = data, hashedFrom = hashedFrom, hashedTo = hashedTo, type = MessageTypes.TEXT_MESSAGE.ordinal)

        SymmetricMessage.encrypt(message, testalias, object : SecretKey {
            override fun getAlgorithm(): String {
                return "TESTALGORITHM"
            }

            override fun getFormat(): String {
                return "TESTFORMAT"
            }

            override fun getEncoded(): ByteArray {
                return byteArrayOf()
            }
        }
        )
    }


    @Test
    fun decryptEncryptMessage() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val data = byteArrayOf(12, 32, 1, 0, 32, 43, 12)
        val message = SymmetricMessage(data = data, hashedFrom = hashedFrom, hashedTo = hashedTo, type = MessageTypes.TEXT_MESSAGE.ordinal)
        val encryptedMessage = SymmetricMessage.encrypt(message, testalias, Crypto.getMyKey())
        val decryptedMessage = SymmetricMessage.decrypt(encryptedMessage!!, Crypto.getMyPublicKey()!!)
        assertNotNull(decryptedMessage)
        assertArrayEquals(data, decryptedMessage!!.data)
        assertEquals(message.hashedFrom, decryptedMessage!!.hashedFrom)
        assertEquals(message.hashedTo, decryptedMessage!!.hashedTo)
        assertEquals(message.type, decryptedMessage!!.type)
    }

    @Test
    fun decryptEncryptMessageInvalidSignature() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val data = byteArrayOf(12, 32, 1, 0, 32, 43, 12)
        val message = SymmetricMessage(data = data, hashedFrom = hashedFrom, hashedTo = hashedTo, type = MessageTypes.TEXT_MESSAGE.ordinal)
        val encryptedMessage = SymmetricMessage.encrypt(message, testalias, Crypto.getMyKey())
        encryptedMessage!!.signature = "invalid"
        val decryptedMessage = SymmetricMessage.decrypt(encryptedMessage!!, Crypto.getMyPublicKey()!!)
        assertNull(decryptedMessage)
    }

    @Test(expected = JSONException::class)
    fun decryptEncryptMessageInvalidExtra() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val data = byteArrayOf(12, 32, 1, 0, 32, 43, 12)
        val message = SymmetricMessage(data = data, hashedFrom = hashedFrom, hashedTo = hashedTo, type = MessageTypes.TEXT_MESSAGE.ordinal)
        val encryptedMessage = SymmetricMessage.encrypt(message, testalias, Crypto.getMyKey())
        encryptedMessage!!.extra = ""
        val decryptedMessage = SymmetricMessage.decrypt(encryptedMessage!!, Crypto.getMyPublicKey()!!)
        assertNotNull(decryptedMessage)
    }

    @Test(expected = AEADBadTagException::class)
    fun decryptEncryptMessageInvalidIv() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val data = byteArrayOf(12, 32, 1, 0, 32, 43, 12)
        val message = SymmetricMessage(data = data, hashedFrom = hashedFrom, hashedTo = hashedTo, type = MessageTypes.TEXT_MESSAGE.ordinal)
        val encryptedMessage = SymmetricMessage.encrypt(message, testalias, Crypto.getMyKey())
        val extras = JSONObject()
        val myKeyByteArray = ByteArray(12)
        SecureRandom().nextBytes(myKeyByteArray)
        extras.put(SymmetricMessage.EXTRA_IV, Base64.encodeToString(myKeyByteArray, Base64.DEFAULT))
        extras.put(SymmetricMessage.EXTRA_ALIAS, testalias)
        encryptedMessage!!.extra = extras.toString()
        val decryptedMessage = SymmetricMessage.decrypt(encryptedMessage!!, Crypto.getMyPublicKey()!!)
        assertNotNull(decryptedMessage)
    }

}