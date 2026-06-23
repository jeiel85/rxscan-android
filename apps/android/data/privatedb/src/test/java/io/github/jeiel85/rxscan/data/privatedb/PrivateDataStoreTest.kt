package io.github.jeiel85.rxscan.data.privatedb

import io.github.jeiel85.rxscan.core.security.AesGcmKeyWrapper
import io.github.jeiel85.rxscan.core.security.DatabaseKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PrivateDataStoreTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun store() = PrivateDataStore(temp.newFolder("private"))

    @Test
    fun wrappedKeyRoundTripsThroughDisk() {
        val store = store()
        val wrapper = AesGcmKeyWrapper(AesGcmKeyWrapper.randomKek())
        val dbKey = DatabaseKey.generate()
        store.persistWrappedKey(wrapper.wrap(dbKey))

        val loaded = store.loadWrappedKey()!!
        assertArrayEquals(dbKey, wrapper.unwrap(loaded))
    }

    @Test
    fun deleteAllRemovesDbKeyImagesAndCache() {
        val store = store()
        store.persistWrappedKey(AesGcmKeyWrapper(AesGcmKeyWrapper.randomKek()).wrap(DatabaseKey.generate()))
        store.databaseFile.writeText("encrypted-db-bytes")
        store.retainedImageFile("rx-1").writeText("img")
        store.cacheDir.mkdirs()
        java.io.File(store.cacheDir, "c.tmp").writeText("x")

        assertTrue(store.hasPrivateData())
        assertTrue(store.deleteAll())

        assertFalse(store.wrappedKeyFile.exists())
        assertFalse(store.databaseFile.exists())
        assertFalse(store.imagesDir.exists())
        assertFalse(store.cacheDir.exists())
        assertFalse(store.hasPrivateData())
    }

    @Test
    fun deletePrescriptionRemovesOnlyThatImage() {
        val store = store()
        store.retainedImageFile("rx-1").writeText("a")
        store.retainedImageFile("rx-2").writeText("b")

        assertTrue(store.deletePrescriptionImage("rx-1"))
        assertFalse(store.retainedImageFile("rx-1").exists())
        assertTrue(store.retainedImageFile("rx-2").exists())
    }

    @Test
    fun destructiveResetClearsEverythingOnKeyInvalidation() {
        val store = store()
        store.persistWrappedKey(AesGcmKeyWrapper(AesGcmKeyWrapper.randomKek()).wrap(DatabaseKey.generate()))
        store.databaseFile.writeText("db")
        assertTrue(store.destructiveReset())
        assertFalse(store.hasPrivateData())
    }

    @Test
    fun unsafePrescriptionIdRejected() {
        val store = store()
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            store.retainedImageFile("../escape")
        }
    }
}
