package nl.healthjournal.data.csv

/** RFC 4180 cell quoting and row splitting, shared by the comma-separated export and import. */
internal object CsvQuoting {
    /** Wraps the cell in double quotes (inner quotes doubled) when it holds a comma, quote or line break. */
    fun cell(text: String?): String =
        if (text == null) {
            ""
        } else if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }

    /** Splits one line on commas, keeping commas and doubled quotes inside quoted cells. Cells are trimmed unless quoted. */
    fun split(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var wasQuoted = false
        var i = 0
        fun finish() {
            cells.add(if (wasQuoted) current.toString() else current.toString().trim())
            current.clear()
            wasQuoted = false
        }
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { current.append('"'); i++ }
                inQuotes && c == '"' -> inQuotes = false
                !inQuotes && c == '"' && current.isBlank() -> { inQuotes = true; wasQuoted = true; current.clear() }
                !inQuotes && c == ',' -> finish()
                else -> current.append(c)
            }
            i++
        }
        finish()
        return cells
    }
}
