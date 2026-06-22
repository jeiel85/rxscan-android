package io.github.jeiel85.rxscan.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootstrapPolicyTest {
    @Test
    fun defaultPolicyKeepsGoalZeroSafetyBoundaries() {
        val policy = BootstrapPolicy()

        assertEquals(26, policy.minSdk)
        assertTrue(policy.requiresMandatoryReview)
        assertFalse(policy.allowsPatientDataBackend)
        assertFalse(policy.allowsGenerativeMedicalReasoning)
    }
}

