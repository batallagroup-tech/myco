package com.batallagroup.myco.core.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestión criptográfica del protocolo Myco.
 * Implementa ECDH (P-256) + AES-256-GCM + ECDSA como capa base compatible con Android 8.0+.
 * Diseñado para migrar a Curve25519/XChaCha20 vía Lazysodium en versiones futuras.
 */
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val EC_ALGORITHM = "EC"
        private const val EC_CURVE = "secp256r1"
        private const val ECDH_ALGORITHM = "ECDH"
        private const val SIGN_ALGORITHM = "SHA256withECDSA"
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val GCM_IV_BYTES = 12
        private const val AES_KEY_BYTES = 32
    }

    // --- Generación de claves ---

    fun generateEncryptionKeyPair(): Pair<ByteArray, ByteArray> {
        val generator = KeyPairGenerator.getInstance(EC_ALGORITHM).apply {
            initialize(ECGenParameterSpec(EC_CURVE))
        }
        val kp = generator.generateKeyPair()
        return Pair(kp.public.encoded, kp.private.encoded)
    }

    fun generateSigningKeyPair(): Pair<ByteArray, ByteArray> {
        val generator = KeyPairGenerator.getInstance(EC_ALGORITHM).apply {
            initialize(ECGenParameterSpec(EC_CURVE))
        }
        val kp = generator.generateKeyPair()
        return Pair(kp.public.encoded, kp.private.encoded)
    }

    // --- Derivación de ID ---

    fun deriveUserId(publicKeyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyBytes)
        return digest.take(4).joinToString("") { "%02x".format(it) }
    }

    // --- Cifrado de mensajes (ECDH + AES-GCM) ---

    fun encryptMessage(
        plaintext: ByteArray,
        recipientPublicKeyBytes: ByteArray,
        senderPrivateKeyBytes: ByteArray
    ): ByteArray {
        val recipientPK = decodePublicKey(recipientPublicKeyBytes)
        val senderSK = decodePrivateKey(senderPrivateKeyBytes)
        val sharedKey = deriveSharedKey(senderSK, recipientPK)

        val iv = java.security.SecureRandom().generateSeed(GCM_IV_BYTES)
        val cipher = Cipher.getInstance(AES_ALGORITHM).apply {
            init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(sharedKey, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
        }
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    fun decryptMessage(
        ciphertext: ByteArray,
        senderPublicKeyBytes: ByteArray,
        recipientPrivateKeyBytes: ByteArray
    ): ByteArray {
        val senderPK = decodePublicKey(senderPublicKeyBytes)
        val recipientSK = decodePrivateKey(recipientPrivateKeyBytes)
        val sharedKey = deriveSharedKey(recipientSK, senderPK)

        val iv = ciphertext.take(GCM_IV_BYTES).toByteArray()
        val data = ciphertext.drop(GCM_IV_BYTES).toByteArray()

        val cipher = Cipher.getInstance(AES_ALGORITHM).apply {
            init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(sharedKey, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
        }
        return cipher.doFinal(data)
    }

    // --- Firma digital (ECDSA) ---

    fun signMessage(message: ByteArray, privateKeyBytes: ByteArray): ByteArray {
        val privateKey = decodePrivateKey(privateKeyBytes)
        return Signature.getInstance(SIGN_ALGORITHM).apply {
            initSign(privateKey)
            update(message)
        }.sign()
    }

    fun verifySignature(message: ByteArray, signature: ByteArray, publicKeyBytes: ByteArray): Boolean {
        return try {
            val publicKey = decodePublicKey(publicKeyBytes)
            Signature.getInstance(SIGN_ALGORITHM).apply {
                initVerify(publicKey)
                update(message)
            }.verify(signature)
        } catch (e: Exception) {
            false
        }
    }

    // --- Helpers ---

    private fun deriveSharedKey(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance(ECDH_ALGORITHM).apply {
            init(privateKey)
            doPhase(publicKey, true)
        }
        val secret = agreement.generateSecret()
        return MessageDigest.getInstance("SHA-256").digest(secret).take(AES_KEY_BYTES).toByteArray()
    }

    private fun decodePublicKey(bytes: ByteArray): PublicKey =
        KeyFactory.getInstance(EC_ALGORITHM).generatePublic(X509EncodedKeySpec(bytes))

    private fun decodePrivateKey(bytes: ByteArray): PrivateKey =
        KeyFactory.getInstance(EC_ALGORITHM).generatePrivate(PKCS8EncodedKeySpec(bytes))

    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    fun fromBase64(str: String): ByteArray = Base64.decode(str, Base64.NO_WRAP)
}
