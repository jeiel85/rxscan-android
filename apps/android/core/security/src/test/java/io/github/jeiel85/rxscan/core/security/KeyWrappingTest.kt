package io.github.jeiel85.rxscan.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyWrappingTest {
    @Test
    fun wrapThenUnwrapRoundTrips() {
        val kek = AesGcmKeyWrapper.randomKek()
        val wrapper = AesGcmKeyWrapper(kek)
        val dbKey = DatabaseKey.generate()
        assertEquals(32, dbKey.size)

        val wrapped = wrapper.wrap(dbKey)
        assertArrayEquals(dbKey, wrapper.unwrap(wrapped))
    }

    @Test
    fun wrappedKeyDoesNotExposePlaintext() {
        val wrapper = AesGcmKeyWrapper(AesGcmKeyWrapper.randomKek())
        val dbKey = DatabaseKey.generate()
        val wrapped = wrapper.wrap(dbKey)
        // Ciphertext must differ from the plaintext key bytes.
        assertFalse(wrapped.ciphertext.toList().containsAll(dbKey.toList()))
    }

    @Test
    fun wrongKeyFailsClosed() {
        val dbKey = DatabaseKey.generate()
        val wrapped = AesGcmKeyWrapper(AesGcmKeyWrapper.randomKek()).wrap(dbKey)
        val otherWrapper = AesGcmKeyWrapper(AesGcmKeyWrapper.randomKek())
        assertThrows(KeyUnwrapException::class.java) { otherWrapper.unwrap(wrapped) }
    }

    @Test
    fun tamperedCiphertextFailsClosed() {
        val wrapper = AesGcmKeyWrapper(AesGcmKeyWrapper.randomKek())
        val wrapped = wrapper.wrap(DatabaseKey.generate())
        wrapped.ciphertext[0] = (wrapped.ciphertext[0].toInt() xor 0xFF).toByte()
        assertThrows(KeyUnwrapException::class.java) { wrapper.unwrap(wrapped) }
    }

    @Test
    fun generatedKeysAreNotConstant() {
        assertFalse(DatabaseKey.generate().contentEquals(DatabaseKey.generate()))
    }
}
