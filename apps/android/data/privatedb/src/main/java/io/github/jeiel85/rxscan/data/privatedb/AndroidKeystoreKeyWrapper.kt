package io.github.jeiel85.rxscan.data.privatedb

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.github.jeiel85.rxscan.core.security.AesGcmKeyWrapper
import io.github.jeiel85.rxscan.core.security.KeyWrapper
import io.github.jeiel85.rxscan.core.security.WrappedKey
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Device [KeyWrapper] whose key-encryption key lives in the Android Keystore
 * (07_SECURITY_PRIVACY.md §4). The KEK never leaves the Keystore; only wrapped key
 * material is persisted. When [requireUserAuthentication] is set (app lock), the OS
 * requires device authentication before the KEK can be used; if the key is
 * permanently invalidated (e.g. biometric change) unwrap fails closed via
 * [io.github.jeiel85.rxscan.core.security.KeyUnwrapException].
 *
 * Device-only: exercised by instrumentation, not JVM unit tests (needs the
 * AndroidKeyStore provider).
 */
class AndroidKeystoreKeyWrapper(
    private val alias: String = DEFAULT_ALIAS,
    private val requireUserAuthentication: Boolean = false,
) : KeyWrapper {
    override fun wrap(keyBytes: ByteArray): WrappedKey = AesGcmKeyWrapper(loadOrCreateKek()).wrap(keyBytes)

    override fun unwrap(wrapped: WrappedKey): ByteArray = AesGcmKeyWrapper(loadOrCreateKek()).unwrap(wrapped)

    private fun loadOrCreateKek(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(requireUserAuthentication)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_ALIAS = "rxscan_private_db_kek"
    }
}
