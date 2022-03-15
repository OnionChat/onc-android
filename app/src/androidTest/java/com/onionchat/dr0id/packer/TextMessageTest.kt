package com.onionchat.dr0id.packer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onionchat.common.Crypto
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.SymmetricMessage
import com.onionchat.dr0id.messaging.messages.TextMessage
import com.onionchat.dr0id.messaging.messages.TextMessageData
import junit.framework.Assert.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextMessageTest {

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
    fun simpleTextMessage() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val teststr = "teststr"
        val message = TextMessage(textData = TextMessageData(teststr, ""), hashedFrom = hashedFrom, hashedTo = hashedTo)
        assertTrue(message.data.isNotEmpty())
        assertThat("encoded data should be bigger then decoded", message.data.size > teststr.length)
    }

    @Test
    fun encodeDecodeTextMessage() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val message = TextMessage(textData = TextMessageData("teststr", "format"), hashedFrom = hashedFrom, hashedTo = hashedTo)
        val secondMessage =
            TextMessage(SymmetricMessage(data = message.data, hashedFrom = hashedFrom, hashedTo = hashedTo, type = MessageTypes.TEXT_MESSAGE.ordinal))
        assertEquals("teststr", secondMessage.getText().text)
        assertEquals("format", secondMessage.getText().formatInfo)
    }

    @Test
    fun encryptTextMessage() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val message = TextMessage(textData = TextMessageData("teststr", "format"), hashedFrom = hashedFrom, hashedTo = hashedTo)
        assertNotNull(SymmetricMessage.encrypt(message, testalias, Crypto.getMyKey()))
    }

    @Test
    fun encryptDecryptTextMessage() {
        val hashedFrom = "from"
        val hashedTo = "to"
        val message = TextMessage(textData = TextMessageData("teststr", "format"), hashedFrom = hashedFrom, hashedTo = hashedTo)
        val secondMessage =
            TextMessage(SymmetricMessage.decrypt(SymmetricMessage.encrypt(message, testalias, Crypto.getMyKey())!!, Crypto.getMyPublicKey()!!)!!)
        assertEquals(message.messageId, secondMessage.messageId)
        assertEquals(message.created, secondMessage.created)
        assertEquals(message.hashedTo, secondMessage.hashedTo)
        assertEquals(message.hashedFrom, secondMessage.hashedFrom)
        assertEquals(message.getText().text, secondMessage.getText().text)
        assertEquals(message.getText().formatInfo, secondMessage.getText().formatInfo)
    }
}