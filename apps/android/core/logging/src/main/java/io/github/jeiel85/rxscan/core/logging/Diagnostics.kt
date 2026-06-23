package io.github.jeiel85.rxscan.core.logging

/**
 * A sensitive value whose [toString] never reveals its content, so accidental
 * string interpolation into a log cannot leak prescription/OCR/PII data
 * (07_SECURITY_PRIVACY.md §3 Logging). Use [reveal] only at a deliberate,
 * non-logging call site.
 */
class SensitiveText(private val raw: String) : SensitiveValue {
    fun reveal(): String = raw
    override fun toString(): String = REDACTED

    companion object {
        const val REDACTED: String = "[REDACTED]"
    }
}

/** Defense-in-depth PII redaction for diagnostics export previews. */
object Redactor {
    private val residentNumber = Regex("""\d{6}-\d{7}""")
    private val phone = Regex("""01\d-?\d{3,4}-?\d{4}""")
    private val email = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    fun redact(text: String): String = text
        .replace(residentNumber, "[주민번호]")
        .replace(phone, "[전화번호]")
        .replace(email, "[이메일]")
}

/**
 * Rotating, size-capped local diagnostics buffer (07_SECURITY_PRIVACY.md §3). Only
 * [SafeLogMessage] values — strings a developer explicitly marked safe — can be
 * recorded, and the export preview is redacted again before it ever leaves the app.
 */
class DiagnosticsBuffer(private val maxEntries: Int = 200) {
    private val entries = ArrayDeque<String>()

    fun record(message: SafeLogMessage) {
        entries.addLast(message.value)
        while (entries.size > maxEntries) entries.removeFirst()
    }

    fun size(): Int = entries.size

    /** Redacted, user-exportable preview (explicit user action only). */
    fun exportPreview(): String = entries.joinToString("\n") { Redactor.redact(it) }

    fun clear() {
        entries.clear()
    }
}
