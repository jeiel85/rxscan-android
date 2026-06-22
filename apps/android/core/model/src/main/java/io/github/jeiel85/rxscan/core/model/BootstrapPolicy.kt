package io.github.jeiel85.rxscan.core.model

data class BootstrapPolicy(
    val minSdk: Int = 26,
    val requiresMandatoryReview: Boolean = true,
    val allowsPatientDataBackend: Boolean = false,
    val allowsGenerativeMedicalReasoning: Boolean = false,
)

