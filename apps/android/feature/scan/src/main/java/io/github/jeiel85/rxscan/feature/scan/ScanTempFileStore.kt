package io.github.jeiel85.rxscan.feature.scan

import java.io.File

/**
 * Temporary scan-image lifecycle (06_LOCAL_DATA_MODEL.md §1/§4). Captured bytes
 * live in app-private no-backup storage and are deleted on cancel, on finalize,
 * and on startup (recovering leftovers from a prior process death). Retained
 * images are handled by the encrypted private store in a later goal — this store
 * never persists anything beyond finalization.
 *
 * Pure [File] logic so cancel/finalize/startup cleanup is unit-testable.
 */
class ScanTempFileStore(private val root: File) {

    init {
        root.mkdirs()
    }

    /** Delete every leftover session directory; returns how many were removed. */
    fun cleanupOnStartup(): Int {
        val sessions = root.listFiles()?.filter { it.isDirectory } ?: emptyList()
        var removed = 0
        for (dir in sessions) {
            if (dir.deleteRecursively()) removed++
        }
        return removed
    }

    fun createSession(sessionId: String): File {
        val dir = sessionDir(sessionId)
        dir.mkdirs()
        return dir
    }

    fun sessionDir(sessionId: String): File = File(root, sanitize(sessionId))

    /** "Discard scan": delete the temporary image, OCR evidence, and session data. */
    fun discardSession(sessionId: String): Boolean = sessionDir(sessionId).deleteRecursively()

    /** Finalize: temporary artifacts are always cleared (retention is the encrypted store's job). */
    fun finalizeSession(sessionId: String): Boolean = sessionDir(sessionId).deleteRecursively()

    fun activeSessions(): List<String> =
        root.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted() ?: emptyList()

    private fun sanitize(sessionId: String): String {
        require(sessionId.isNotBlank()) { "sessionId must not be blank" }
        require(sessionId.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            "sessionId must be a safe identifier"
        }
        return sessionId
    }
}
