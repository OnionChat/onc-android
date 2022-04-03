package com.onionchat.dr0id.messaging.keyexchange

import android.util.Base64
import com.onionchat.common.AddUserPayload
import com.onionchat.common.Crypto
import com.onionchat.common.Crypto.decryptAsym
import com.onionchat.common.Crypto.encryptSym
import com.onionchat.common.Crypto.generateSymmetricKey
import com.onionchat.common.Crypto.sign
import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.localstorage.messagestore.EncryptedMessage
import org.json.JSONObject
import java.security.Key
import java.security.cert.Certificate
import java.util.*

class ResponsePubMessage(
    hashedFrom: String,
    hashedTo: String,
    messageId: String = java.util.UUID.randomUUID().toString(),
    signature: String = "",
    created: Long = System.currentTimeMillis(),
    messageStatus: Int = 0,
    val addUserPayload: AddUserPayload,
    type: Int = MessageTypes.RESPONSE_PUB_MESSAGE.ordinal
) : IMessage(messageId, hashedFrom, hashedTo, signature, messageStatus, created, 0, type, "") {


    fun toEncryptedMessage(targetPub: Certificate, myPrivate: Key): EncryptedMessage? {
        val symAlias = UUID.randomUUID().toString()
        val tmpKeyByts = generateSymmetricKey(symAlias)
        val symKey = Crypto.getSymmetricKey(symAlias)
        if (symKey == null) {
            Logging.e(TAG, "Unable to retrieve symmetric key $symAlias")
            return null
        }
        // encrypt payload
        val data = AddUserPayload.encode(addUserPayload).toByteArray()
        val encryptionResult = encryptSym(symKey, data)
        val encryptedPayload = encryptionResult.encryptedData

        // encrypt key
        val encryptedKey = Crypto.encryptAsym(targetPub, tmpKeyByts)

        val pubMessage = JSONObject()
        pubMessage.put(KEY_ENCRYPTED_PAYLOAD, Base64.encodeToString(encryptedPayload, Base64.DEFAULT))
        pubMessage.put(KEY_IV, Base64.encodeToString(encryptionResult.iv, Base64.DEFAULT))
        pubMessage.put(KEY_ALIAS, symAlias)
        pubMessage.put(KEY_ENCRYPTED_KEY, Base64.encodeToString(encryptedKey, Base64.DEFAULT))

        val messageBytes = pubMessage.toString().toByteArray()
        sign(myPrivate, messageBytes)?.let {
            signature = Base64.encodeToString(it, Base64.DEFAULT)
            return EncryptedMessage(
                messageId,
                messageBytes,
                hashedFrom,
                hashedTo,
                signature,
                0,
                created,
                0,
                type
            )
        } ?: run {
            return null
        }
    }

    companion object {

        val TAG = "ResponsePubMessage"

        val KEY_ENCRYPTED_PAYLOAD = "payload"
        val KEY_IV = "iv"
        val KEY_ALIAS = "alias"
        val KEY_ENCRYPTED_KEY = "encrypted_key"

        fun fromEncryptedMessage(encryptedMessage: EncryptedMessage, myPrivate: Key): ResponsePubMessage? {
            encryptedMessage.encryptedMessageBytes?.let { data ->
                val json = JSONObject(String(data))

                if (!json.has(KEY_ENCRYPTED_PAYLOAD) ||
                    !json.has(KEY_IV) ||
                    !json.has(KEY_ENCRYPTED_KEY) ||
                    !json.has(KEY_ALIAS)
                ) {
                    Logging.e(TAG, "fromEncryptedMessage [-] invalid message $json")
                    return null
                }

                val encryptedPayload = Base64.decode(json.getString(KEY_ENCRYPTED_PAYLOAD), Base64.DEFAULT)
                val iv = Base64.decode(json.getString(KEY_IV), Base64.DEFAULT)
                val encryptedKey = Base64.decode(json.getString(KEY_ENCRYPTED_KEY), Base64.DEFAULT)
                val alias = json.getString(KEY_ALIAS)
                decryptAsym(myPrivate, encryptedKey)?.let { key ->
                    val symKey = Crypto.storeSymmetricKey(alias, key)
                    if (symKey == null) {
                        Logging.e(TAG, "Unable to store symmetric key")
                        return null
                    }
                    val decryptedPayload = Crypto.decryptSym(symKey, iv, encryptedPayload)
                    if(decryptedPayload == null) {
                        Logging.e(TAG, "Unable to decrypt payload")
                        return null
                    }

                    // verify even the pub was shipped within the message
                    //if (verify(data, encryptedMessage.encryptedMessageBytes, Base64.decode(encryptedMessage.signature, Base64.DEFAULT))) {
                    AddUserPayload.decode(String(decryptedPayload))?.let {
                        return@fromEncryptedMessage ResponsePubMessage(
                            encryptedMessage.hashedFrom,
                            encryptedMessage.hashedTo,
                            encryptedMessage.messageId,
                            encryptedMessage.signature,
                            encryptedMessage.created,
                            encryptedMessage.messageStatus,
                            it
                        )
                    } ?: run {
                        Logging.e(TAG, "Error while verify message")
                        return@fromEncryptedMessage null
                    }


                } ?: run {
                    Logging.e(TAG, "Decryption error")
                    return null
                }
            }
        }
    }

    override fun getMessageType(): Int {
        TODO("Not yet implemented")
    }
}