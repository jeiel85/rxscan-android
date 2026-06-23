package io.github.jeiel85.rxscan.engine.parser

import io.github.jeiel85.rxscan.core.model.DosageForm
import io.github.jeiel85.rxscan.core.model.ReleaseForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MedicineLineExtractorTest {
    @Test
    fun extractsStrengthFormAndManufacturer() {
        val fields = MedicineLineExtractor.extract("암로디핀정 5mg 한미약품", "암로디핀정 5mg 한미약품")
        assertEquals(5.0, fields.strength!!.value, 1e-9)
        assertEquals("mg", fields.strength!!.unit)
        assertEquals(DosageForm.TABLET, fields.dosageForm)
        assertEquals("한미약품", fields.manufacturer)
        assertEquals("암로디핀정", fields.productName)
    }

    @Test
    fun detectsExtendedRelease() {
        val fields = MedicineLineExtractor.extract("딜라트렌서방정 8mg", "딜라트렌서방정 8mg")
        assertEquals(ReleaseForm.EXTENDED, fields.releaseForm)
    }

    @Test
    fun missingStrengthIsNullNotInferred() {
        val fields = MedicineLineExtractor.extract("타이레놀정", "타이레놀정")
        assertNull(fields.strength)
        assertEquals(DosageForm.TABLET, fields.dosageForm)
        // Absence of "서방" must not be read as IMMEDIATE.
        assertEquals(ReleaseForm.UNKNOWN, fields.releaseForm)
    }

    @Test
    fun capsuleFormDetected() {
        val fields = MedicineLineExtractor.extract("오메가캡슐 1000mg", "오메가캡슐 1000mg")
        assertEquals(DosageForm.CAPSULE, fields.dosageForm)
        assertEquals(1000.0, fields.strength!!.value, 1e-9)
    }

    @Test
    fun rawLineIsPreserved() {
        val fields = MedicineLineExtractor.extract("RAW 텍스트", "raw 텍스트")
        assertEquals("RAW 텍스트", fields.rawLine)
        assertEquals("raw 텍스트", fields.normalizedLine)
    }
}
