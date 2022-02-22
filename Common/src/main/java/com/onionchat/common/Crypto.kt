package com.onionchat.common

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.*
import javax.security.auth.x500.X500Principal

object Crypto {

    val ANDROID_KEYSTORE = "AndroidKeyStore"
    val MY_KEY_ALIAS = "MyOnionKeyPair"
    private lateinit var keyPair: KeyPair

    fun generateKey() {
        //We create the start and expiry date for the key
        val startDate = GregorianCalendar()
        val endDate = GregorianCalendar()
        endDate.add(Calendar.YEAR, 5)

        //We are creating a RSA key pair and store it in the Android Keystore
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)

        //We are creating the key pair with sign and verify purposes
        val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            MY_KEY_ALIAS,
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

        //Initialization of key generator with the parameters we have specified above
        keyPairGenerator.initialize(parameterSpec)

        //Generates the key pair
        keyPair = keyPairGenerator.genKeyPair()
    }

    private fun signData() {
        try {
            //We get the Keystore instance
            val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
            }

            //Retrieves the private key from the keystore
            val privateKey: PrivateKey = keyStore.getKey(MY_KEY_ALIAS, null) as PrivateKey
            //keyStore.getCertificate(KEY_ALIAS) as Certificate
            //We sign the data with the private key. We use RSA algorithm along SHA-256 digest algorithm
//            val signature: ByteArray? = Signature.getInstance("SHA256withRSA").run {
//                initSign(privateKey)
//                update("TestString".toByteArray())
//                sign()
//            }
//
//            if (signature != null) {
//                //We encode and store in a variable the value of the signature
//                signatureResult = Base64.encodeToString(signature, Base64.DEFAULT)
//            }

        } catch (e: UserNotAuthenticatedException) {
            //Exception thrown when the user has not been authenticated
        } catch (e: KeyPermanentlyInvalidatedException) {
            //Exception thrown when the key has been invalidated for example when lock screen has been disabled.
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

//    fun encryptData(alias: String, dataIn: ByteArray) : ByteArray {
//
//    }
//
//    fun decryptData(alias: String, dataIn: ByteArray) : ByteArray {
//
//    }

    fun getMyPublicKey() : Certificate? {
        return getPublicKey(MY_KEY_ALIAS)
    }

    fun getMyKey() : Key {
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        return keyStore.getKey(MY_KEY_ALIAS,null)!!
    }

    fun getPublicKey(alias: String) : Certificate? {
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        var cert: Certificate? = keyStore.getCertificate(alias) as Certificate?
        return cert;
    }

    fun storePublicKey(alias: String, pubKey: ByteArray?) {
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        if(pubKey == null) {
            keyStore.deleteEntry(alias)
            return
        }
        val cert: Certificate? = CertificateFactory.getInstance("X.509").generateCertificate(
            ByteArrayInputStream(pubKey))
        keyStore.setCertificateEntry(alias, cert)
    }

    fun hash(byteArray: ByteArray) : ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(byteArray)
    }

    fun pubKeyHash(pubKey: ByteArray) : String {
        return android.util.Base64.encodeToString(hash(pubKey), android.util.Base64.DEFAULT)
    }
}