package nl.healthjournal.app.settings

import nl.healthjournal.app.settings.MetricSlot.ACTIVITY
import nl.healthjournal.app.settings.MetricSlot.BLOOD_PRESSURE
import nl.healthjournal.app.settings.MetricSlot.GLUCOSE
import nl.healthjournal.app.settings.MetricSlot.MIGRAINE
import nl.healthjournal.app.settings.MetricSlot.WAIST_CIRCUMFERENCE
import nl.healthjournal.app.settings.MetricSlot.WEIGHT
import nl.healthjournal.app.ui.history.HistoryFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetricSlotTest {

    @Test
    fun `the default order puts Migraine after Glucose and before Waist and Activity`() {
        assertEquals(listOf(WEIGHT, BLOOD_PRESSURE, GLUCOSE, MIGRAINE, WAIST_CIRCUMFERENCE, ACTIVITY), MetricSlot.DEFAULT_ORDER)
    }

    @Test
    fun `nothing stored or an empty value gives the default order`() {
        assertEquals(MetricSlot.DEFAULT_ORDER, MetricSlot.parseOrder(null))
        assertEquals(MetricSlot.DEFAULT_ORDER, MetricSlot.parseOrder(""))
    }

    @Test
    fun `a saved order is read back`() {
        val order = listOf(ACTIVITY, MIGRAINE, GLUCOSE, WEIGHT, WAIST_CIRCUMFERENCE, BLOOD_PRESSURE)

        assertEquals(order, MetricSlot.parseOrder(MetricSlot.serialiseOrder(order)))
    }

    @Test
    fun `an unknown name is dropped without resetting the rest`() {
        assertEquals(
            listOf(GLUCOSE, WEIGHT, BLOOD_PRESSURE, MIGRAINE, WAIST_CIRCUMFERENCE, ACTIVITY),
            MetricSlot.parseOrder("GLUCOSE, SLEEP ,WEIGHT")
        )
    }

    @Test
    fun `a repeated name counts once, at its first place`() {
        assertEquals(
            listOf(GLUCOSE, WEIGHT, BLOOD_PRESSURE, MIGRAINE, WAIST_CIRCUMFERENCE, ACTIVITY),
            MetricSlot.parseOrder("GLUCOSE,WEIGHT,GLUCOSE")
        )
    }

    @Test
    fun `metrics missing from the stored value follow in their default order`() {
        assertEquals(
            listOf(ACTIVITY, WEIGHT, BLOOD_PRESSURE, GLUCOSE, MIGRAINE, WAIST_CIRCUMFERENCE),
            MetricSlot.parseOrder("ACTIVITY")
        )
    }

    @Test
    fun `every slot has a history filter and only Migraine has no logging type`() {
        assertEquals(MetricSlot.entries.size, MetricSlot.entries.map { it.historyFilter }.toSet().size)
        assertEquals(listOf(MIGRAINE), MetricSlot.entries.filter { it.metricType == null })
        assertEquals(HistoryFilter.MIGRAINE, MIGRAINE.historyFilter)
        assertNull(MIGRAINE.metricType)
    }
}
