package nl.healthjournal.app.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.domain.shared.model.UnitSystem
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.ValidationField
import nl.healthjournal.domain.shared.command.ValidationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class DomainErrorUiTextTest {

    private fun names(dir: String): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/$dir/strings.xml"))
        val nodes = doc.documentElement.childNodes
        return (0 until nodes.length).mapNotNull { (nodes.item(it) as? Element)?.takeIf { e -> e.tagName == "string" }?.getAttribute("name") }.toSet()
    }

    private val en = names("values")
    private val nl = names("values-nl")

    private fun nameOf(id: Int): String =
        R.string::class.java.fields.first { it.getInt(null) == id }.name

    private val imperial = DisplayUnits(UnitSystem.IMPERIAL, DisplayUnits.DEFAULT.glucose)

    @Test
    fun `every rule and field resolves to a string in english and dutch`() {
        val all = ValidationRule.entries.flatMap { rule ->
            ValidationField.entries.map { field -> DomainError.Invalid(field, rule) }
        } + DomainError.NotFound("weight", "x") + DomainError.Storage()
        for (error in all) {
            val text = error.toUiText() as UiText.Res
            val name = nameOf(text.id)
            assertTrue("$error -> $name missing in values", name in en)
            assertTrue("$error -> $name missing in values-nl", name in nl)
        }
    }

    @Test
    fun `out of range messages are specific per field`() {
        fun id(field: ValidationField) =
            (DomainError.Invalid(field, ValidationRule.OUT_OF_RANGE).toUiText() as UiText.Res).id
        assertEquals(R.string.log_err_systolic, id(ValidationField.SYSTOLIC))
        assertEquals(R.string.log_err_diastolic, id(ValidationField.DIASTOLIC))
        assertEquals(R.string.log_err_glucose, id(ValidationField.GLUCOSE))
        assertEquals(R.string.log_err_waist, id(ValidationField.WAIST))
        assertEquals(R.string.history_edit_invalid, id(ValidationField.PULSE))
    }

    @Test
    fun `weight and distance messages carry the display unit`() {
        val weight = DomainError.Invalid(ValidationField.WEIGHT, ValidationRule.OUT_OF_RANGE)
        assertEquals(UiText.Res(R.string.log_err_weight, "kg"), weight.toUiText())
        assertEquals(UiText.Res(R.string.log_err_weight, "lb"), weight.toUiText(imperial))
        val distance = DomainError.Invalid(ValidationField.DISTANCE, ValidationRule.OUT_OF_RANGE)
        assertEquals(UiText.Res(R.string.log_err_distance, "mi"), distance.toUiText(imperial))
    }

    @Test
    fun `end before start, not found and storage use their own messages`() {
        assertEquals(
            UiText.Res(R.string.migraine_err_end_before_start),
            DomainError.Invalid(ValidationField.TIME_RANGE, ValidationRule.END_NOT_AFTER_START).toUiText()
        )
        assertEquals(UiText.Res(R.string.history_err_not_found), DomainError.NotFound("weight", "x").toUiText())
        assertEquals(UiText.Res(R.string.log_err_save_failed), DomainError.Storage().toUiText())
    }
}
