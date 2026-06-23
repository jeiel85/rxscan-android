package io.github.jeiel85.rxscan.core.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTest {
    @Test
    fun sensitiveTextNeverRevealsContentViaToString() {
        val secret = SensitiveText("타이레놀정 500mg")
        assertEquals(SensitiveText.REDACTED, secret.toString())
        // Accidental interpolation cannot leak the content.
        val interpolated = "약: $secret"
        assertEquals("약: ${SensitiveText.REDACTED}", interpolated)
        assertFalse(interpolated.contains("타이레놀"))
        assertEquals("타이레놀정 500mg", secret.reveal())
    }

    @Test
    fun redactorMasksPii() {
        val redacted = Redactor.redact("환자 900101-1234567 전화 010-1234-5678 메일 a@b.com")
        assertTrue(redacted.contains("[주민번호]"))
        assertTrue(redacted.contains("[전화번호]"))
        assertTrue(redacted.contains("[이메일]"))
        assertFalse(redacted.contains("900101-1234567"))
        assertFalse(redacted.contains("010-1234-5678"))
    }

    @Test
    fun diagnosticsBufferRotatesAndRedactsExport() {
        val buffer = DiagnosticsBuffer(maxEntries = 3)
        repeat(5) { buffer.record(SafeLogMessage("event $it")) }
        assertEquals(3, buffer.size())

        buffer.record(SafeLogMessage("연락처 010-1111-2222"))
        val preview = buffer.exportPreview()
        assertTrue(preview.contains("[전화번호]"))
        assertFalse(preview.contains("010-1111-2222"))
    }

    @Test
    fun clearEmptiesBuffer() {
        val buffer = DiagnosticsBuffer()
        buffer.record(SafeLogMessage("x"))
        buffer.clear()
        assertEquals(0, buffer.size())
    }
}
