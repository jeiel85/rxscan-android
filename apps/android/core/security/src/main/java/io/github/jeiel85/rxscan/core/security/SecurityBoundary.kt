package io.github.jeiel85.rxscan.core.security

data class SecurityBoundary(
    val cleartextTrafficAllowed: Boolean = false,
    val privateBackupAllowed: Boolean = false,
)

