package com.onionchat.dr0id.messaging

import com.onionchat.common.Logging
import com.onionchat.common.MessageTypes
import com.onionchat.dr0id.messaging.keyexchange.RequestPubMessage
import com.onionchat.dr0id.messaging.keyexchange.ResponsePubMessage
import com.onionchat.dr0id.messaging.keyexchange.NegotiateSymKeyMessage
import com.onionchat.dr0id.messaging.messages.*
import com.onionchat.localstorage.messagestore.EncryptedMessage
import java.security.Key
import java.security.cert.Certificate

object MessageProcessor {

    val TAG = "MessageProcessor"

    val MESSAGE_UID = "uid"
    val MESSAGE_SIGNATURE = "signature"
    val MESSAGE_VERSION = "version"

    val MESSAGE_TEXT_MESSAGE = "text_message"
    val MESSAGE_READ_MESSAGE = "message_read"
    val MESSAGE_BROADCAST_TEXT_MESSAGE = "broadcast_text_message"

//    fun retrieveMessages(myUserId: String, fromUser: User) : List<Message> {
//        val unparsedMessages = gate.retrieveInbox(myUserId, fromUser.id)
//
//        gate.deleteConversation(myUserId, fromUser.id)
//        return decodeMessages(unparsedMessages);
//    }
//
//    fun decodeMessage(message: String): DecryptedMessage {
//        if (message.length == 0) {
//            return InvalidMessage()
//        }
//        val obj = JSONObject(message)
//        val hashedUid = obj.getString(MESSAGE_UID)
//        val signature_str = obj.getString(MESSAGE_SIGNATURE)
//
//
//        val message: DecryptedMessage? = if (obj.has(MESSAGE_TEXT_MESSAGE)) {
//            TextMessage(decryptMessage(hashedUid, obj.getString(MESSAGE_TEXT_MESSAGE), signature_str), hashedUid, signature_str)
//        } else if (obj.has(MESSAGE_READ_MESSAGE)) {
//            MessageReadMessage(decryptMessage(hashedUid, obj.getString(MESSAGE_READ_MESSAGE), signature_str), hashedUid, signature_str)
//        } else if (obj.has(MESSAGE_BROADCAST_TEXT_MESSAGE)) {
//            BroadcastTextMessage.createFromMessageString(
//                decryptMessage(hashedUid, obj.getString(MESSAGE_BROADCAST_TEXT_MESSAGE), signature_str),
//                hashedUid,
//                signature_str
//            )
//        } else {
//            InvalidMessage()
//        }
//        if (message == null) {
//            return InvalidMessage()
//        }
//        return message
//    }
//
//    fun encodeMessage(message: DecryptedMessage, keyId: String): String {
//        return encryptMessage(message, keyId); // TODO BASE64 !!
//    }
//
//    internal fun encryptMessage(message: DecryptedMessage, keyId: String): String {
//        Logging.d("MessageManager", "encryptMessage message text len <" + message.messageBytes.size + ">")
//        var messageBytes = message.messageBytes
//        if (messageBytes.size > 150) {
//            messageBytes = message.messageBytes.copyOf(150)
//            Logging.e("MessageManager", "!! Warning !! - message too long. Cutting it. <" + messageBytes.size + ">")
//        }
//        // encrypt using targets key
//        val cert: Certificate? = Crypto.getPublicKey(keyId)
//        return cert?.publicKey?.let {
//            // 1. encrypt with partner public
//            val cipher: Cipher = Cipher.getInstance(ASYMMETRIC_ALGORITHM)
//            cipher.init(Cipher.ENCRYPT_MODE, it)
//            val encryptedBytes = cipher.doFinal(messageBytes)
//            val res = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
//            Logging.d("MessageManager", "encryptMessage cipher len <" + res.toByteArray().size + ">")
//
//
//            // 2. sign it with my private
//            val mykey = Crypto.getMyKey()
//
//            val sig: Signature = Signature.getInstance(SIGNATURE_ALGORITHM)
//            sig.initSign(mykey as PrivateKey);
//            sig.update(encryptedBytes);
//            val signatureBytes = sig.sign()
//            val signature = Base64.encodeToString(signatureBytes, Base64.DEFAULT)
//
//            // 3. wrap it
//            val obj = JSONObject()
//            obj.put(MESSAGE_UID, IDGenerator.toVisibleId(UserManager.myId!!))
//            if (message is BroadcastTextMessage) {
//                obj.put(MESSAGE_BROADCAST_TEXT_MESSAGE, res)
//            } else if (message is TextMessage) {
//                obj.put(MESSAGE_TEXT_MESSAGE, res)
//            } else if (message is MessageReadMessage) {
//                obj.put(MESSAGE_READ_MESSAGE, res)
//            } else {
//                "ERROR"
//            }
//            message.signature = signature // ui can later identify message read status
//
//            obj.put(MESSAGE_SIGNATURE, signature)
//            obj.put(MESSAGE_VERSION, BuildConfig.VERSION_CODE)
//            val finalMessage = obj.toString()
//            Logging.d("MessageManager", "total message size <" + finalMessage.toByteArray().size + ">")
//
//            finalMessage
//        } ?: "ERROR"
//    }
//
//    internal fun decryptMessage(hashedUid: String, encrypted_message: String, signature_str: String): String {
//        val cert = Crypto.getMyKey()
//        return cert.let {
//
//            // extract the wrapper
//
//            var fromUser: User? = null
//            UserManager.getAllUsers().get().forEach {
//                if (hashedUid.equals(IDGenerator.toVisibleId(it.id))) {
//                    fromUser = it
//                }
//            }
//            if (fromUser == null) {
//                Logging.e("MessageManager", "Unable to receive message... user <" + hashedUid + "> not found in database")
//                return ""
//            }
//            // 1. check signature
//            val publicKey = Crypto.getPublicKey(fromUser!!.certId)
//            val encrypted_bytes = Base64.decode(encrypted_message, Base64.DEFAULT)
//            val signature_bytes = Base64.decode(signature_str, Base64.DEFAULT)
//            Logging.d("MessageManager", "decryptMessage - signature_bytes len <" + signature_bytes.size + "> <" + signature_str + ">")
//            val publicSignature = Signature.getInstance(SIGNATURE_ALGORITHM)
//            publicSignature.initVerify(publicKey);
//            publicSignature.update(encrypted_bytes);
//            if (!publicSignature.verify(signature_bytes)) {
//                "SIGNATURE CHECK FAILED"
//            }
//
//            // 2. decrypt with my private key
//            Logging.d("MessageManager", "decryptMessage inner cipher len <" + encrypted_bytes.size + "> <" + encrypted_message + ">")
//            val cipher: Cipher = Cipher.getInstance(ASYMMETRIC_ALGORITHM)
//            cipher.init(Cipher.DECRYPT_MODE, it)
//            val decryptedBytes = cipher.doFinal(encrypted_bytes)
//            Logging.d("MessageManager", "decryptMessage bytes <" + String(decryptedBytes).length + ">")
//
//            String(decryptedBytes)
//        } ?: "ERROR"
//    }


    fun pack(message: IMessage, targetPub: Certificate?, myPrivate: Key?, symmetricAlias: String?): EncryptedMessage? {
        Logging.d(TAG, "pack [+] message=$message targetPub=$targetPub myPrivate=$myPrivate symalias=$symmetricAlias")
        if (message is SymmetricMessage) {
            if (myPrivate == null) {
                Logging.e(TAG, "pack [-] SymmetricMessage needs myPrivate set [-] unable to send message")
                return null;
            }
            if (symmetricAlias == null) {
                Logging.e(TAG, "pack [-] SymmetricMessage needs symmetricAlias set [-] unable to send message")
                return null;
            }
            return SymmetricMessage.encrypt(message, symmetricAlias, myPrivate)
        } else if (message is AsymmetricMessage) {
            if (targetPub == null) {
                Logging.e(TAG, "pack [-] AsymmetricMessage needs targetPub set [-] unable to send message")
                return null;
            }
            if (myPrivate == null) {
                Logging.e(TAG, "pack [-] AsymmetricMessage needs myPrivate set [-] unable to send message")
                return null;
            }
            return AsymmetricMessage.encrypt(message, targetPub, myPrivate)
        } else if (message is RequestPubMessage) {
            return message.toEncryptedMessage()
        } else if (message is ResponsePubMessage) {
            if (targetPub == null) {
                Logging.e(TAG, "pack [-] ResponsePubMessage message needs targetPub set [-] unable to send message")
                return null;
            }
            if (myPrivate == null) {
                Logging.e(TAG, "pack [-] ResponsePubMessage message needs myPrivate set [-] unable to send message")
                return null;
            }
            return message.toEncryptedMessage(targetPub, myPrivate)
        }
        Logging.e(TAG, "pack [-] Unsupported message type <" + message + ">");
        return null;
    }


    fun unpack(encryptedMessage: EncryptedMessage, sourcePub: Certificate?, myPrivate: Key?): IMessage? {
        Logging.d(TAG, "unpack [+] encryptedMessage=$encryptedMessage sourcePub=$sourcePub myPrivate=$myPrivate")

        when (encryptedMessage.type) {
            MessageTypes.REQUEST_PUB_MESSAGE.ordinal -> {
                return RequestPubMessage.fromEncryptedMessage(encryptedMessage)
            }
            MessageTypes.RESPONSE_PUB_MESSAGE.ordinal -> {
                if (myPrivate == null) {
                    Logging.e(TAG, "unpack [-] ResponsePubMessage needs myPrivate set [-] unable to send message")
                    return null;
                }
                return ResponsePubMessage.fromEncryptedMessage(encryptedMessage, myPrivate)
            }
            MessageTypes.SYM_KEY_MESSAGE.ordinal -> {
                if (sourcePub == null) {
                    Logging.e(TAG, "unpack [-] AsymmetricMessage needs targetPub set [-] unable to send message")
                    return null;
                }
                if (myPrivate == null) {
                    Logging.e(TAG, "unpack [-] AsymmetricMessage needs myPrivate set [-] unable to send message")
                    return null;
                }
                val message = AsymmetricMessage.decrypt(encryptedMessage, myPrivate, sourcePub)?.let { NegotiateSymKeyMessage(it) }
                if (message == null) {
                    Logging.e(TAG, "unpack [-] unable to decrypt and parse SymKeyMessage")
                }
                return message
            }
            MessageTypes.TEXT_MESSAGE.ordinal -> {
//                if (sourcePub == null) {
//                    Logging.e(TAG, "unpack [-] TextMessage needs sourcePub set [-] unable to send message") // todo how to deal with it
//                    //return null;
//                }

                val message = SymmetricMessage.decrypt(encryptedMessage, sourcePub)?.let { TextMessage(it) }
                if (message == null) {
                    Logging.e(TAG, "unpack [-] unable to decrypt and parse TextMessage")
                }
                return message
            }
            MessageTypes.BROADCAST_TEXT_MESSAGE.ordinal -> {
                if (sourcePub == null) {
                    Logging.e(TAG, "unpack [-] TextMessage needs sourcePub set [-] unable to send message") // todo how to deal with it
                    //return null;
                }

                val message = SymmetricMessage.decrypt(encryptedMessage, sourcePub)?.let { BroadcastTextMessage(it) }
                if (message == null) {
                    Logging.e(TAG, "unpack [-] unable to decrypt and parse BroadcastTextMessage")
                }
                return message
            }
            MessageTypes.MESSAGE_READ_MESSAGE.ordinal -> {
                if (sourcePub == null) {
                    Logging.e(TAG, "unpack [-] MessageReadMessage needs sourcePub set [-] unable to send message")
                    return null;
                }

                val message = SymmetricMessage.decrypt(encryptedMessage, sourcePub)?.let { MessageReadMessage(it) }
                if (message == null) {
                    Logging.e(TAG, "unpack [-] unable to decrypt and parse MessageReadMessage")
                }
                return message
            }
            MessageTypes.REQUEST_CONTACT_DETAILS_MESSAGE.ordinal -> {
                if (sourcePub == null) {
                    Logging.e(TAG, "unpack [-] RequestContactDetailsMessage needs sourcePub set [-] unable to send message")
                    return null;
                }

                val message = SymmetricMessage.decrypt(encryptedMessage, sourcePub)?.let { RequestContactDetailsMessage(it) }
                if (message == null) {
                    Logging.e(TAG, "unpack [-] unable to decrypt and parse RequestContactDetailsMessage")
                }
                return message
            }
            MessageTypes.PROVIDE_CONTACT_DETAILS_MESSAGE.ordinal -> {
                if (sourcePub == null) {
                    Logging.e(TAG, "unpack [-] ProvideContactDetailsMessage needs sourcePub set [-] unable to send message")
                    return null;
                }

                val message = SymmetricMessage.decrypt(encryptedMessage, sourcePub)?.let { ProvideContactDetailsMessage(it) }
                if (message == null) {
                    Logging.e(TAG, "unpack [-] unable to decrypt and parse ProvideContactDetailsMessage")
                }
                return message
            }
            MessageTypes.ATTACHMENT_MESSAGE.ordinal -> {
                if (sourcePub == null) {
                    Logging.e(TAG, "unpack [-] AttachmentMessage needs sourcePub set [-] unable to send message")
                    return null;
                }

                val message = SymmetricMessage.decrypt(encryptedMessage, sourcePub)?.let { AttachmentMessage(it) }
                if (message == null) {
                    Logging.e(TAG, "unpack [-] unable to decrypt and parse AttachmentMessage")
                }
                return message
            }
            MessageTypes.PROVIDE_SYM_KEY_MESSAGE.ordinal -> {
                if (sourcePub == null) {
                    Logging.e(TAG, "unpack [-] ProvideSymKeyMessage needs sourcePub set [-] unable to send message")
                    return null;
                }

                val message = SymmetricMessage.decrypt(encryptedMessage, sourcePub)?.let { ProvideSymKeyMessage(it) }
                if (message == null) {
                    Logging.e(TAG, "unpack [-] unable to decrypt and parse ProvideSymKeyMessage")
                }
                return message
            }
            MessageTypes.REQUEST_SYN_KEY_MESSAGE.ordinal -> {
                if (sourcePub == null) {
                    Logging.e(TAG, "unpack [-] RequestSymKeyMessage needs sourcePub set [-] unable to send message")
                    return null;
                }

                val message = SymmetricMessage.decrypt(encryptedMessage, sourcePub)?.let { RequestSymKeyMessage(it) }
                if (message == null) {
                    Logging.e(TAG, "unpack [-] unable to decrypt and parse RequestSymKeyMessage")
                }
                return message
            }
        }
        Logging.e(TAG, "unpack [-] Unsupported message type <$encryptedMessage>");
        return null
    }

}