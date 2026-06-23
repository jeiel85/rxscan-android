package io.github.jeiel85.rxscan.feature.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ScanTempFileStoreTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun store() = ScanTempFileStore(temp.newFolder("scan-temp"))

    @Test
    fun createSessionMakesDirectory() {
        val s = store()
        val dir = s.createSession("session-1")
        assertTrue(dir.isDirectory)
        assertEquals(listOf("session-1"), s.activeSessions())
    }

    @Test
    fun discardDeletesSessionFilesAndEvidence() {
        val s = store()
        val dir = s.createSession("session-1")
        java.io.File(dir, "capture.jpg").writeText("bytes")
        java.io.File(dir, "ocr-evidence.json").writeText("{}")

        assertTrue(s.discardSession("session-1"))
        assertFalse(dir.exists())
        assertEquals(emptyList<String>(), s.activeSessions())
    }

    @Test
    fun finalizeClearsTemporaryArtifacts() {
        val s = store()
        val dir = s.createSession("session-2")
        java.io.File(dir, "capture.jpg").writeText("bytes")

        assertTrue(s.finalizeSession("session-2"))
        assertFalse(dir.exists())
    }

    @Test
    fun startupCleanupRemovesLeftoversFromPriorProcessDeath() {
        val root = temp.newFolder("scan-temp")
        // Simulate a prior process that died mid-scan, leaving two session dirs.
        ScanTempFileStore(root).apply {
            createSession("dead-1").also { java.io.File(it, "capture.jpg").writeText("x") }
            createSession("dead-2")
        }

        val fresh = ScanTempFileStore(root)
        assertEquals(2, fresh.cleanupOnStartup())
        assertEquals(emptyList<String>(), fresh.activeSessions())
    }

    @Test
    fun rejectsUnsafeSessionIds() {
        val s = store()
        assertThrows(IllegalArgumentException::class.java) { s.createSession("../escape") }
        assertThrows(IllegalArgumentException::class.java) { s.createSession("") }
    }
}
