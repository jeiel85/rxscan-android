package io.github.jeiel85.rxscan.core.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Wrapped database-key material: AES-GCM ciphertext plus its nonce. */
class WrappedKey(val ciphertext: ByteArray, val nonce: ByteArray)

/** Thrown when unwrapping fails (wrong/invalidated key-encryption key or tampering). Fail closed. */
class KeyUnwrapException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Wraps/unwraps the random private-DB key with a key-encryption key (KEK).
 * On device the KEK lives in the Android Keystore (07_SECURITY_PRIVACY.md §4); the
 * AES-GCM logic here is provider-agnostic so it is unit-testable on the JVM.
 */
interface KeyWrapper {
    fun wrap(keyBytes: ByteArray): WrappedKey
    fun unwrap(wrapped: WrappedKey): ByteArray
}

/** Generates the random 256-bit private-DB key (07_SECURITY_PRIVACY.md §4). */
object DatabaseKey {
    const val SIZE_BYTES: Int = 32

    fun generate(random: SecureRandom = SecureRandom()): ByteArray =
        ByteArray(SIZE_BYTES).also { random.nextBytes(it) }
}

/**
 * AES-GCM key wrapping. The auth tag makes a wrong KEK or tampered ciphertext fail
 * closed (KeyUnwrapException) rather than returning garbage key bytes — the private
 * DB then cannot be opened without the correct protected key.
 */
class AesGcmKeyWrapper(private val kek: SecretKey) : KeyWrapper {
    override fun wrap(keyBytes: ByteArray): WrappedKey {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, kek)
        val ciphertext = cipher.doFinal(keyBytes)
        return WrappedKey(ciphertext = ciphertext, nonce = cipher.iv.copyOf())
    }

    override fun unwrap(wrapped: WrappedKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(TAG_BITS, wrapped.nonce))
            return cipher.doFinal(wrapped.ciphertext)
        } catch (e: java.security.GeneralSecurityException) {
            throw KeyUnwrapException("Private key could not be unwrapped (fail closed)", e)
        }
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128

        /** Generates an AES-256 KEK (the device uses an equivalent Keystore-resident key). */
        fun randomKek(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }
}
