package dev.antomanc.tameki.core.data.csv

/**
 * RFC 4180-ish CSV reader: quoted fields may span newlines, `""` escapes quotes,
 * separator is `;` or `,` (detected from the header row), BOM is stripped.
 */
object CsvReader {
    fun parse(text: String): ParsedCsv {
        val normalized = text.removePrefix("\uFEFF")
        if (normalized.isBlank()) return ParsedCsv(emptyList(), ',')
        val separator = detectSeparator(firstLogicalLine(normalized))
        val rows = mutableListOf<List<String>>()
        val current = StringBuilder()
        val fields = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < normalized.length) {
            val ch = normalized[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < normalized.length && normalized[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == separator && !inQuotes -> {
                    fields += current.toString()
                    current.clear()
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && i + 1 < normalized.length && normalized[i + 1] == '\n') {
                        i++
                    }
                    fields += current.toString()
                    current.clear()
                    if (fields.any { it.isNotEmpty() } || fields.size > 1) {
                        rows += fields.toList()
                    }
                    fields.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        if (current.isNotEmpty() || fields.isNotEmpty() || inQuotes) {
            fields += current.toString()
            if (fields.any { it.isNotEmpty() } || fields.size > 1) {
                rows += fields.toList()
            }
        }
        return ParsedCsv(rows, separator)
    }

    fun detectSeparator(headerLine: String): Char {
        var inQuotes = false
        var semis = 0
        var commas = 0
        for (ch in headerLine) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                !inQuotes && ch == ';' -> semis++
                !inQuotes && ch == ',' -> commas++
            }
        }
        return if (semis > commas) ';' else ','
    }

    private fun firstLogicalLine(text: String): String {
        var inQuotes = false
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    out.append(ch)
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> break
                else -> out.append(ch)
            }
            i++
        }
        return out.toString()
    }
}

data class ParsedCsv(
    val rows: List<List<String>>,
    val separator: Char,
)
