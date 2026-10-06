package nl.healthjournal.domain.model.medication

import nl.healthjournal.domain.model.metrics.EntryComment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class MedicationValidationTest {

    @Test
    fun `name is trimmed and limited to 80 characters`() {
        assertEquals("Medication A", MedicationName.of("  Medication A ").value)
        assertEquals(80, MedicationName.of("a".repeat(80)).value.length)
        assertThrows(IllegalArgumentException::class.java) { MedicationName.of("a".repeat(81)) }
        assertThrows(IllegalArgumentException::class.java) { MedicationName.of("   ") }
    }

    @Test
    fun `dose must be greater than 0 and at most 1000`() {
        Dosage(null, BigDecimal("0.5"), DoseUnit.ML)
        Dosage(null, BigDecimal(1000), DoseUnit.ML)
        assertThrows(IllegalArgumentException::class.java) { Dosage(null, BigDecimal.ZERO, DoseUnit.ML) }
        assertThrows(IllegalArgumentException::class.java) { Dosage(null, BigDecimal("1000.1"), DoseUnit.ML) }
    }

    @Test
    fun `strength must be greater than 0`() {
        assertThrows(IllegalArgumentException::class.java) { Strength(BigDecimal.ZERO, StrengthUnit.MG) }
        assertThrows(IllegalArgumentException::class.java) { Strength(BigDecimal("-1"), StrengthUnit.MG) }
    }

    @Test
    fun `strength and dose units are independent and never converted`() {
        val dosage = Dosage(Strength(BigDecimal("500"), StrengthUnit.MG), BigDecimal("2"), DoseUnit.TABLETS)
        assertEquals(StrengthUnit.MG, dosage.strength?.unit)
        assertEquals(DoseUnit.TABLETS, dosage.doseUnit)
        assertEquals(BigDecimal("2"), dosage.amountPerIntake)
    }

    @Test
    fun `ids accept version 4 and reject other versions`() {
        MedicationId.generate()
        IntakeId.generate()
        assertThrows(IllegalArgumentException::class.java) {
            MedicationId.fromString("00000000-0000-1000-8000-000000000000")
        }
    }

    @Test
    fun `a planned intake can be taken or skipped`() {
        val id = MedicationId.generate()
        val planned = LocalDate.of(2026, 5, 4).atTime(8, 0)
        Intake(IntakeId.generate(), id, planned, IntakeStatus.TAKEN, Instant.now())
        Intake(IntakeId.generate(), id, planned, IntakeStatus.SKIPPED, null)
    }

    @Test
    fun `an as needed dose needs a time taken and cannot be skipped`() {
        val id = MedicationId.generate()
        Intake(IntakeId.generate(), id, null, IntakeStatus.TAKEN, Instant.now())
        assertThrows(IllegalArgumentException::class.java) {
            Intake(IntakeId.generate(), id, null, IntakeStatus.TAKEN, null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Intake(IntakeId.generate(), id, null, IntakeStatus.SKIPPED, null)
        }
    }

    @Test
    fun `a skipped intake has no time taken and no actual amount`() {
        val id = MedicationId.generate()
        val planned = LocalDate.of(2026, 5, 4).atTime(8, 0)
        assertThrows(IllegalArgumentException::class.java) {
            Intake(IntakeId.generate(), id, planned, IntakeStatus.SKIPPED, Instant.now())
        }
        assertThrows(IllegalArgumentException::class.java) {
            Intake(IntakeId.generate(), id, planned, IntakeStatus.SKIPPED, null, actualAmount = BigDecimal.ONE)
        }
    }

    @Test
    fun `actual amount and comment are optional and validated`() {
        val id = MedicationId.generate()
        val planned = LocalDate.of(2026, 5, 4).atTime(8, 0)
        val intake = Intake(
            IntakeId.generate(), id, planned, IntakeStatus.TAKEN, Instant.now(),
            actualAmount = BigDecimal("1.5"), comment = EntryComment.ofOrNull("left side")
        )
        assertEquals("left side", intake.comment?.text)
        assertThrows(IllegalArgumentException::class.java) {
            Intake(IntakeId.generate(), id, planned, IntakeStatus.TAKEN, Instant.now(), actualAmount = BigDecimal.ZERO)
        }
    }
}
