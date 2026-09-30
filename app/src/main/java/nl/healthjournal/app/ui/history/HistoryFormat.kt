package nl.healthjournal.app.ui.history

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Formats a stored UTC instant in the device time zone using the app's region formats. */
fun Instant.formatLocal(): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(this)
