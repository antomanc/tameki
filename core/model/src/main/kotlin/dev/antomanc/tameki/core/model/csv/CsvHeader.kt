package dev.antomanc.tameki.core.model.csv

/**
 * Reads the header line of a CSV exported by a bank or another app: strips a UTF-8 BOM, detects
 * the delimiter (comma, semicolon or tab, whichever splits the line most outside quotes) and
 * honours quoted names, so "Data;Importo;Descrizione" gives three columns, not one.
 */
object CsvHeader {
    private const val BOM = '\uFEFF'
    private val Candidates = listOf(',', ';', '\t')

    fun delimiter(line: String): Char =
        Candidates.maxByOrNull { d -> countOutsideQuotes(line, d) }?.takeIf { countOutsideQuotes(line, it) > 0 } ?: ','

    fun parse(line: String): List<String> {
        val clean = line.trimStart(BOM)
        if (clean.isBlank()) return emptyList()
        val d = delimiter(clean)
        val out = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < clean.length) {
            val c = clean[i]
            when {
                c == '"' && quoted && i + 1 < clean.length && clean[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                c == '"' -> quoted = !quoted
                c == d && !quoted -> {
                    out += cell.toString().trim()
                    cell.clear()
                }
                else -> cell.append(c)
            }
            i++
        }
        out += cell.toString().trim()
        return out
    }

    private fun countOutsideQuotes(line: String, d: Char): Int {
        var quoted = false
        var count = 0
        for (c in line) {
            if (c == '"') quoted = !quoted else if (c == d && !quoted) count++
        }
        return count
    }
}
