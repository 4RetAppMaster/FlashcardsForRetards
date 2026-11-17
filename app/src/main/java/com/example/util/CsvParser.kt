package com.example.util

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {
    fun parse(inputStream: InputStream): List<List<String>> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 16 * 1024)
        val result = mutableListOf<List<String>>()
        var delimiter: Char? = null
        var isFirstLine = true

        reader.forEachLine { rawLine ->
            val cleaned = if (isFirstLine) {
                isFirstLine = false
                rawLine.removePrefix("\uFEFF")
            } else {
                rawLine
            }
            if (cleaned.isNotBlank()) {
                val activeDelimiter = delimiter ?: detectDelimiter(cleaned).also { delimiter = it }
                val parsedRow = parseLine(cleaned, activeDelimiter)
                if (parsedRow.isNotEmpty()) {
                    result.add(parsedRow)
                }
            }
        }
        return result
    }

    private fun detectDelimiter(firstLine: String): Char {
        var inQuotes = false
        var hasComma = false
        var hasSemicolon = false
        for (c in firstLine) {
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (c == ',') hasComma = true
                else if (c == ';') hasSemicolon = true
            }
        }
        return if (!hasComma && hasSemicolon) ';' else ','
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val result = ArrayList<String>(4)
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        val length = line.length
        while (i < length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }
}
