package io.github.jeiel85.rxscan.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanStateMachineTest {
    @Test
    fun happyPathReachesFinalized() {
        val path = listOf(
            ScanState.IDLE,
            ScanState.CAPTURING,
            ScanState.QUALITY_CHECKED,
            ScanState.PREPROCESSING,
            ScanState.OCR_RUNNING,
            ScanState.PARSING,
            ScanState.MATCHING,
            ScanState.REVIEW_REQUIRED,
            ScanState.CONFIRMED,
            ScanState.FINALIZED,
        )
        var current = path.first()
        for (next in path.drop(1)) {
            current = ScanStateMachine.transition(current, next)
        }
        assertEquals(ScanState.FINALIZED, current)
    }

    @Test
    fun cancelAllowedFromAnyNonTerminalState() {
        for (state in ScanState.entries) {
            val expected = state !in ScanStateMachine.terminalStates
            assertEquals(
                "CANCELLED from $state",
                expected,
                ScanStateMachine.canTransition(state, ScanState.CANCELLED),
            )
        }
    }

    @Test
    fun failClosedAllowedFromAnyNonTerminalState() {
        assertTrue(ScanStateMachine.canTransition(ScanState.OCR_RUNNING, ScanState.FAILED_CLOSED))
        assertTrue(ScanStateMachine.canTransition(ScanState.REVIEW_REQUIRED, ScanState.FAILED_CLOSED))
        assertFalse(ScanStateMachine.canTransition(ScanState.FINALIZED, ScanState.FAILED_CLOSED))
    }

    @Test
    fun failRecoverableOnlyFromProcessingStates() {
        assertTrue(ScanStateMachine.canTransition(ScanState.OCR_RUNNING, ScanState.FAILED_RECOVERABLE))
        assertFalse(ScanStateMachine.canTransition(ScanState.IDLE, ScanState.FAILED_RECOVERABLE))
        assertFalse(ScanStateMachine.canTransition(ScanState.CONFIRMED, ScanState.FAILED_RECOVERABLE))
    }

    @Test
    fun recoverableFailureCanRetryCapture() {
        assertTrue(ScanStateMachine.canTransition(ScanState.FAILED_RECOVERABLE, ScanState.CAPTURING))
    }

    @Test
    fun reviewCannotBeBypassedToFinalized() {
        assertFalse(ScanStateMachine.canTransition(ScanState.MATCHING, ScanState.CONFIRMED))
        assertFalse(ScanStateMachine.canTransition(ScanState.MATCHING, ScanState.FINALIZED))
        assertFalse(ScanStateMachine.canTransition(ScanState.REVIEW_REQUIRED, ScanState.FINALIZED))
    }

    @Test
    fun terminalStatesRejectAllTransitions() {
        for (terminal in ScanStateMachine.terminalStates) {
            for (target in ScanState.entries) {
                assertFalse("$terminal -> $target", ScanStateMachine.canTransition(terminal, target))
            }
        }
    }

    @Test
    fun invalidTransitionThrows() {
        val error = assertThrows(InvalidScanTransition::class.java) {
            ScanStateMachine.transition(ScanState.IDLE, ScanState.FINALIZED)
        }
        assertEquals(ScanState.IDLE, error.from)
        assertEquals(ScanState.FINALIZED, error.to)
    }
}
