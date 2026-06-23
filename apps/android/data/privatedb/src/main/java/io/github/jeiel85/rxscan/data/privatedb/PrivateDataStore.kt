package io.github.jeiel85.rxscan.data.privatedb

import io.github.jeiel85.rxscan.core.security.WrappedKey
import java.io.File
import java.nio.ByteBuffer

/**
 * Filesystem lifecycle for the private (encrypted) domain (06_LOCAL_DATA_MODEL.md
 * §4, 07_SECURITY_PRIVACY.md §3). Holds the encrypted DB file, the wrapped key, and
 * encrypted retained images under an app-private root.
 *
 * The DB is encrypted with the random key that only exists wrapped on disk; without
 * the Keystore KEK to unwrap it, the DB cannot be opened (fail closed). Deletion
 * destroys the wrapped key first, which is the primary protection for retained data.
 */
class PrivateDataStore(private val root: File) {
    val databaseFile: File = File(root, "private.db")
    val wrappedKeyFile: File = File(root, "wrapped-key.bin")
    val imagesDir: File = File(root, "images")
    val cacheDir: File = File(root, "cache")

    init {
        root.mkdirs()
    }

    fun persistWrappedKey(wrapped: WrappedKey) {
        val buffer = ByteBuffer.allocate(4 + wrapped.nonce.size + wrapped.ciphertext.size)
        buffer.putInt(wrapped.nonce.size)
        buffer.put(wrapped.nonce)
        buffer.put(wrapped.ciphertext)
        wrappedKeyFile.writeBytes(buffer.array())
    }

    fun loadWrappedKey(): WrappedKey? {
        if (!wrappedKeyFile.exists()) return null
        val bytes = wrappedKeyFile.readBytes()
        val buffer = ByteBuffer.wrap(bytes)
        val nonceSize = buffer.getInt()
        val nonce = ByteArray(nonceSize).also { buffer.get(it) }
        val ciphertext = ByteArray(buffer.remaining()).also { buffer.get(it) }
        return WrappedKey(ciphertext = ciphertext, nonce = nonce)
    }

    fun retainedImageFile(prescriptionId: String): File {
        require(prescriptionId.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            "unsafe prescription id"
        }
        imagesDir.mkdirs()
        return File(imagesDir, "$prescriptionId.enc")
    }

    /** "Delete prescription": remove its retained encrypted image (DB rows handled by the DB layer). */
    fun deletePrescriptionImage(prescriptionId: String): Boolean =
        retainedImageFile(prescriptionId).let { if (it.exists()) it.delete() else true }

    /**
     * "Delete all data": private DB, wrapped key, retained images, and caches
     * (06_LOCAL_DATA_MODEL.md §4). The wrapped key is destroyed so any residual DB
     * bytes are unrecoverable.
     */
    fun deleteAll(): Boolean {
        val results = listOf(
            wrappedKeyFile.deleteIfExists(),
            databaseFile.deleteIfExists(),
            imagesDir.deleteRecursivelyIfExists(),
            cacheDir.deleteRecursivelyIfExists(),
        )
        return results.all { it }
    }

    /** Key invalidation recovery (07_SECURITY_PRIVACY.md §4): never bypass encryption — reset. */
    fun destructiveReset(): Boolean = deleteAll()

    fun hasPrivateData(): Boolean =
        wrappedKeyFile.exists() || databaseFile.exists() || imagesDir.exists()

    private fun File.deleteIfExists(): Boolean = if (exists()) delete() else true
    private fun File.deleteRecursivelyIfExists(): Boolean = if (exists()) deleteRecursively() else true
}
