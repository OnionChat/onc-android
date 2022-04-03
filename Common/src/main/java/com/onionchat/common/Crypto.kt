package com.onionchat.common

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.AlgorithmParameterSpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal


object Crypto {

    val ANDROID_KEYSTORE = "AndroidKeyStore"
    val MY_KEY_ALIAS = "MyOnionKeyPair"
    val MY_SYM_KEY_ALIAS = "MyOnionSymmetricKey"
    private lateinit var keyPair: KeyPair
    val SYMMETRIC_ALGORITHM = "AES/GCM/NoPadding"
    val ASYMMETRIC_ALGORITHM = "RSA/ECB/PKCS1Padding"
    val SIGNATURE_ALGORITHM = "SHA256withRSA"

    const val SYMMETRIC_BUFF_SIZE = 2048

    val TAG = "Crypto"

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun generateKey(alias: String = MY_KEY_ALIAS) {
        if (getPublicKey(alias) != null) {
            Logging.e(TAG, "generateKey [-] key already exists <$alias>.. abort.")
            return
        }
        //We create the start and expiry date for the key
        val startDate = GregorianCalendar()
        val endDate = GregorianCalendar()
        endDate.add(Calendar.YEAR, 5)

        //We are creating a RSA key pair and store it in the Android Keystore
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)

        //We are creating the key pair with sign and verify purposes
        val parameterSpec: AlgorithmParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).run {
            setCertificateSerialNumber(BigInteger.valueOf(777))       //Serial number used for the self-signed certificate of the generated key pair, default is 1
            setCertificateSubject(X500Principal("CN=OnionChat"))     //Subject used for the self-signed certificate of the generated key pair, default is CN=fake
            setDigests(KeyProperties.DIGEST_SHA256)                         //Set of digests algorithms with which the key can be used
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1) // todo do key exchange and use padding
            setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1) // todo do key exchange and use padding
            setCertificateNotBefore(startDate.time)                         //Start of the validity period for the self-signed certificate of the generated, default Jan 1 1970
            setCertificateNotAfter(endDate.time)                            //End of the validity period for the self-signed certificate of the generated key, default Jan 1 2048
            //            setUserAuthenticationRequired(true)                             //Sets whether this key is authorized to be used only if the user has been authenticated, default false
            //            setUserAuthenticationValidityDurationSeconds(30)                //Duration(seconds) for which this key is authorized to be used after the user is successfully authenticated
            build()
        }
//        } else {
//            val start = Calendar.getInstance()
//            val end = Calendar.getInstance()
//            end.add(Calendar.YEAR, 1)
//            val spec = KeyPairGeneratorSpec.Builder(context)
//                .setAlias(MY_KEY_ALIAS)
//                .setSubject(
//                    X500Principal(
//                        "CN=OnionChat"
//                    )
//                )
//                .setSerialNumber(BigInteger.ONE)
//                .setStartDate(start.time)
//                .setEndDate(end.time)
//                .build()
//            KeyPairGenerator.getInstance(KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
//            spec
//        }

        //Initialization of key generator with the parameters we have specified above
        keyPairGenerator.initialize(parameterSpec)

        //Generates the key pair
        keyPair = keyPairGenerator.genKeyPair()
    }


    data class EncryptionResult(val encryptedData: ByteArray, val iv: ByteArray, val size:Int)

    fun encryptSym(secretKey: SecretKey, inputStream: InputStream, outputStream : OutputStream): EncryptionResult {
        val cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val buff = ByteArray(SYMMETRIC_BUFF_SIZE)

        var readTotal = 0
        var read = 0
        while (read >= 0) {
            read = inputStream.read(buff)
            if (read > 0) {
                val update = cipher.update(buff, 0, read)
                if (update != null) {
                    outputStream.write(update)
                }
                readTotal += read
            }
        }
        val final = cipher.doFinal()
        outputStream.write(final)
        return EncryptionResult(byteArrayOf(), cipher.iv, readTotal)
    }

    fun encryptSym(secretKey: SecretKey, data: ByteArray): EncryptionResult {

        val inputStream = ByteArrayInputStream(data)
        val outputStream = ByteArrayOutputStream()
        val res = encryptSym(secretKey, inputStream, outputStream)
        return EncryptionResult(outputStream.toByteArray(), res.iv, res.size)
    }

    fun decryptSym(key: Key, iv: ByteArray, data: ByteArray): ByteArray? {
        Logging.d(TAG, "decryptSym(${key.algorithm}, ${iv.size}, ${data.size})")
        val cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val os = ByteArrayOutputStream()
        val buffsize = SYMMETRIC_BUFF_SIZE
        var off = 0
        while (off < data.size) {
            var toRead = buffsize
            if (off + buffsize > data.size) {
                toRead = data.size - off
            }
            val inserted = cipher.update(data, off, toRead)
            if (inserted != null) {
                os.write(inserted, 0, inserted.size)
            }
            off += toRead
        }
        val final = cipher.doFinal()
        os.write(final, 0, final.size)
        val res = os.toByteArray()
        Logging.d(TAG, "decryptSym(${key.algorithm}, ${iv.size}, ${data.size}) res ${res.size}")
        return res
    }

    fun encryptAsym(pub: Certificate, data: ByteArray): ByteArray {
        Logging.d(TAG, "encryptAsym(${pub.type}, ${data.size}")
        val cipher = Cipher.getInstance(Crypto.ASYMMETRIC_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, pub)
        return cipher.doFinal(data)
    }

    fun decryptAsym(key: Key, data: ByteArray): ByteArray? {
        Logging.d(TAG, "decryptAsym(${key.algorithm}, ${data.size})")
        val cipher = Cipher.getInstance(Crypto.ASYMMETRIC_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    fun sign(key: Key, data: ByteArray): ByteArray? {
        val sig: Signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        sig.initSign(key as PrivateKey);
        sig.update(data);
        return sig.sign()
    }

    fun verify(pub: Certificate, data: ByteArray, signature_bytes: ByteArray): Boolean {
        val publicSignature = Signature.getInstance(SIGNATURE_ALGORITHM)
        publicSignature.initVerify(pub);
        publicSignature.update(data);
        return publicSignature.verify(signature_bytes)
    }

    fun getMyPublicKey(): Certificate? {
        return getPublicKey(MY_KEY_ALIAS)
    }

    fun getMyKey(): Key {
        return getKey(MY_KEY_ALIAS)!!
    }

    fun getKey(alias: String): Key? {
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        return keyStore.getKey(alias, null)
    }

    fun getPublicKey(alias: String): Certificate? {
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        return keyStore.getCertificate(alias)
    }

    fun storePublicKey(alias: String, pubKey: ByteArray?) {
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        if (pubKey == null) {
            keyStore.deleteEntry(alias)
            return
        }
        val cert: Certificate? = byteToPub(pubKey)
        keyStore.setCertificateEntry(alias, cert)
    }

    fun byteToPub(pubKey: ByteArray): Certificate? {
        return CertificateFactory.getInstance("X.509").generateCertificate(
            ByteArrayInputStream(pubKey)
        )
    }

    fun byteToSymmetricKey(symmetricKeyBytes: ByteArray): SecretKey? {
        if (symmetricKeyBytes.isEmpty()) return null
        return SecretKeySpec(symmetricKeyBytes, 0, symmetricKeyBytes.size, "AES");
    }

    fun hash(byteArray: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(byteArray)
    }

    fun pubKeyHash(pubKey: ByteArray): String {
        return android.util.Base64.encodeToString(hash(pubKey), android.util.Base64.DEFAULT)
    }

    fun hasAlias(alias: String): Boolean {
        KeyStore.getInstance(ANDROID_KEYSTORE)?.let {
            return it.containsAlias(alias)
        }
        return false
    }

    fun getMySymmetricKey(): SecretKey? {
        return getSymmetricKey(MY_SYM_KEY_ALIAS)
    }

    fun getSymmetricKey(alias: String): SecretKey? {
        Logging.d(TAG, "getSymmetricKey [+] get symmetric key with alias <$alias>")
        KeyStore.getInstance(ANDROID_KEYSTORE)?.let {
            it.load(null)
            val key = it.getKey(alias, null)
            return if (key !is SecretKey) {
                Logging.e(TAG, "Invalid symmetric key <$key>")
                null
            } else {
                key
            }
        }
        return null
    }

    fun storeSymmetricKey(alias: String, symmetricKeyBytes: ByteArray?): SecretKey? {
        Logging.d(TAG, "storeSymmetricKey [+] store symmetric key with alias <$alias> len=<${symmetricKeyBytes?.size}>")
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)

//            ?.let {
//                val key = it.getKey(alias, null)
//                return if (key !is SecretKey) {
//                    Logging.e(TAG, "Invalid symmetric key <$key>")
//                    null
//                } else {
//                    key
//                }
//            }
        keyStore.load(null)

        if (symmetricKeyBytes == null) {
            keyStore.deleteEntry(alias)
            return null
        }

//        val key: SecretKey? = byteToSymmetricKey(symmetricKeyBytes)
//        keyStore.setKeyEntry(alias, symmetricKeyBytes, null)
        val aesKey: SecretKey = SecretKeySpec(symmetricKeyBytes, 0, symmetricKeyBytes.size, "AES")

        val protection: KeyStore.ProtectionParameter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        } else {
            KeyStore.PasswordProtection("DUMMY_PASSWORD".toCharArray())
        }
        keyStore.setEntry(
            alias,
            KeyStore.SecretKeyEntry(aesKey),
            protection
        )

        return getSymmetricKey(alias)
    }

    fun generateSymmetricKey(alias: String): ByteArray {
        Logging.d(TAG, "generateSymmetricKey [+] generate symmetric key with alias <$alias>")
//        val startDate = GregorianCalendar()
//        val endDate = GregorianCalendar()
//        endDate.add(Calendar.YEAR, 5)
//
//        //We are creating a RSA key pair and store it in the Android Keystore
//        val keyGenerator: KeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
//
//        //We are creating the key pair with sign and verify purposes
//        val parameterSpec: AlgorithmParameterSpec = KeyGenParameterSpec.Builder(
//            alias,
//            KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
//        ).run {
//            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
//            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
//
//            //setCertificateSerialNumber(BigInteger.valueOf(777))       //Serial number used for the self-signed certificate of the generated key pair, default is 1
//            //setCertificateSubject(X500Principal("CN=OnionChat"))     //Subject used for the self-signed certificate of the generated key pair, default is CN=fake
//            //setDigests(KeyProperties.DIGEST_SHA256)                         //Set of digests algorithms with which the key can be used
////            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1) // todo do key exchange and use padding
//            //setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1) // todo do key exchange and use padding
//            //setCertificateNotBefore(startDate.time)                         //Start of the validity period for the self-signed certificate of the generated, default Jan 1 1970
//            //setCertificateNotAfter(endDate.time)                            //End of the validity period for the self-signed certificate of the generated key, default Jan 1 2048
////            setUserAuthenticationRequired(true)                             //Sets whether this key is authorized to be used only if the user has been authenticated, default false
////            setUserAuthenticationValidityDurationSeconds(30)                //Duration(seconds) for which this key is authorized to be used after the user is successfully authenticated
//            build()
//        }
//
//        //Initialization of key generator with the parameters we have specified above
//        keyGenerator.init(parameterSpec)
        val myKeyByteArray = ByteArray(32)
        SecureRandom().nextBytes(myKeyByteArray)
        Logging.d(TAG, "generateSymmetricKey [+] generate symmetric key with alias <$alias> len=<${myKeyByteArray?.size}>")

        val aesKey: SecretKey = SecretKeySpec(myKeyByteArray, 0, myKeyByteArray.size, "AES")
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val protection: KeyStore.ProtectionParameter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        } else {
            KeyStore.PasswordProtection("DUMMY_PASSWORD".toCharArray())
        }
        keyStore.setEntry(
            alias,
            KeyStore.SecretKeyEntry(aesKey),
            protection
        )

        //Generates the key pair
        return myKeyByteArray
    }
}