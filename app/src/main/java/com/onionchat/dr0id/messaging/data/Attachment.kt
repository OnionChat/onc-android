package com.onionchat.dr0id.messaging.data

import android.content.Context
import android.net.Uri
import com.onionchat.common.Crypto
import com.onionchat.common.Crypto.encryptSym
import com.onionchat.common.Logging
import com.onionchat.dr0id.database.MessageManager
import com.onionchat.dr0id.mediaprocessing.ImageProcessor.createMediaThumbnail
import com.onionchat.localstorage.PathProvider
import java.io.File
import java.io.IOException
import java.security.cert.Certificate
import java.util.*

data class Attachment(
    val attachmentId: String,
    val mimetype: String,
    val signature: ByteArray,
    val size: Int,
    val key: ByteArray,
    val iv: ByteArray,
    val alias: String,
    val thumbnail: ByteArray? = null,
    val quotedMessageId: String = "",
    val extra: String = ""
) {
    // attachment management
    var cachedAttachmentBytes: ByteArray? = null

    fun isDownloaded(context: Context): Boolean {
        val file = buildAttachmentPath(context, this)
        if (!file.exists()) {
            Logging.e(TAG, "isDownloaded [-] file doesn't exist ${file.absolutePath}")
            return false
        }
        val downloadedSize = file.length()
        if (downloadedSize < size) {
            Logging.e(TAG, "isDownloaded [-] size doesn't match <$downloadedSize> < <$size>")
            return false // ! todo not fully downloaded !!
        }
        return true
    }

    fun loadEncryptedData(context: Context): ByteArray? {
        val attachmentPath = buildAttachmentPath(context, this)
        return try {
            attachmentPath.inputStream().readBytes()
        } catch (exception: Exception) {
            Logging.e(MessageManager.TAG, "loadAttachmentDataFromDisk [-] unable to find file ${attachmentPath.absolutePath}")
            null
        }
    }

    fun verify(context: Context, cert: Certificate): Boolean {
        loadEncryptedData(context)?.let {
            return verify(cert, it)
        }
        return false // unable to verify
    }


    fun verify(cert: Certificate, encryptedBytes: ByteArray): Boolean {
        if (!Crypto.verify(cert, encryptedBytes, signature)) {
            Logging.e(TAG, "decryptAttachmentData [-] signature check of attachment failed !! $this")
            return false
        }
        return true
    }

    fun loadDecryptedData(
        cert: Certificate?,
        context: Context
    ): ByteArray? {
        Logging.d(MessageManager.TAG, "loadDecryptedData [+] decrypt attachment <${this}> : <${cert}>")
        if (!isDownloaded(context)) {
            Logging.e(TAG, "loadDecryptedData [-] attachment is not downloaded")
            return null
        }

        val encryptedBytes = loadEncryptedData(context)
        if (encryptedBytes == null) {
            Logging.e(TAG, "loadDecryptedData [-] unable to load encrypted data")
            return null
        }
        try {
            // check signature
            if (cert != null && !verify(context, cert)) { // todo how to deal with unable to verify?
                Logging.e(TAG, "loadDecryptedData [-] signature check of attachment failed !! $this")
                return null
            }
            val secretKey = Crypto.storeSymmetricKey(alias, key)
            if (secretKey == null) {
                Logging.e(MessageManager.TAG, "loadDecryptedData [-] unable to decrypt message")
                return null
            }
            Crypto.decryptSym(secretKey, iv, encryptedBytes)?.let { decryptedBytes ->
                if (size != decryptedBytes.size) {
                    Logging.e(MessageManager.TAG, "loadDecryptedData [-] attachment size doesn't match !! $this")
                }
                cachedAttachmentBytes = decryptedBytes
                return@loadDecryptedData decryptedBytes
            } ?: run {
                Logging.e(MessageManager.TAG, "loadDecryptedData [-] unable to decrypt message")
            }

        } catch (e: java.lang.Exception) {
            Logging.e(MessageManager.TAG, "loadDecryptedData [-] unable to decrypt attachment", e)
        }
        return null
    }

    companion object {

        const val TAG = "Attachment"
        const val LOCAL_URI = "local_video_uri"

        fun buildAttachmentPath(context: Context, attachmentMessageMetaData: Attachment): File {
            return File(PathProvider.getAttachmentPath(context).absolutePath + File.separator + attachmentMessageMetaData.attachmentId)
        }

        fun buildAttachmentPath(context: Context, attachmentId: String): File {
            return File(PathProvider.getAttachmentPath(context).absolutePath + File.separator + attachmentId)
        }

        fun encryptLargeAttachment(encryptedAttachmentPath: File, keyAlias: String, context: Context, uri: Uri?): Crypto.EncryptionResult {
            if (uri == null) {
                throw IllegalArgumentException("encryptLargeAttachment [-] uri must not be null. Abort.")
            }
            Logging.d(TAG, "encryptLargeAttachment [+] going to encrypt large attachment of uri <${uri}>")

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                throw IOException("encryptLargeAttachment [-] Unable to encrypt attachment <${uri}>. Error while open stream")
            }

            val key = Crypto.getSymmetricKey(keyAlias)
            if (key == null) {
                throw Exception("encryptLargeAttachment [-] Unable to get get symmetric key")
            }

            val outputStream = encryptedAttachmentPath.outputStream()
            val result = encryptSym(key, inputStream, outputStream)
            return result
        }

        fun create(
            context: Context,
            unencryptedData: ByteArray? = null,
            uri: Uri? = null,
            mimetype: String
        ): Attachment? {
            val attachmentId = UUID.randomUUID().toString()
            val keyAlias = UUID.randomUUID().toString()
            val attachmentPath = buildAttachmentPath(context, attachmentId)

            val thumbnail: ByteArray? = createMediaThumbnail(unencryptedData, uri, mimetype)

            try {
                // 1. generate symmetric key
                val keyData = Crypto.generateSymmetricKey(keyAlias)
                val secretKey = Crypto.getSymmetricKey(keyAlias) ?: throw Exception("Unable to get generate symmetric key")
                // 2. encrypt
                val encryptionResult = if (unencryptedData != null) {
                    val encryptionResult = encryptSym(secretKey, unencryptedData)
                    val encryptedData = encryptionResult.encryptedData
                    // 3. write to disk
                    val outputStream = attachmentPath.outputStream()
                    outputStream.write(encryptedData)
                    outputStream.close()
                    encryptionResult
                } else if (uri != null) {
                    encryptLargeAttachment(attachmentPath, keyAlias, context, uri)
                } else {
                    throw IllegalArgumentException("unencryptedData and uri is null. One of both must be provided")
                }



                if (!attachmentPath.exists()) {
                    throw Exception(" unable to write encrypted message to disk <${attachmentPath}>")
                }
                // 4. sign
                val signature = if (encryptionResult.encryptedData.size > 0) {
                    Crypto.sign(Crypto.getMyKey(), encryptionResult.encryptedData) ?: throw Exception("Unable to sign message")
                } else {
                    byteArrayOf() // todo sign entire video !!!!!!!
                }

                // 5. create object
                val iv = encryptionResult.iv
                return Attachment(
                    attachmentId,
                    mimetype,
                    signature,
                    encryptionResult.size,
                    keyData,
                    iv,
                    keyAlias,
                    thumbnail
                )

            } catch (exception: Exception) {
                Logging.e(MessageManager.TAG, "create [-] unable to create attachment <${attachmentId}, ${mimetype}>", exception)
                return null
            }
            Logging.e(
                MessageManager.TAG,
                "createAttachmentMetadata [-] unable to write encrypted message to disk <${attachmentId}, ${mimetype}, ${attachmentPath}>"
            )
            return null
        }
    }
}