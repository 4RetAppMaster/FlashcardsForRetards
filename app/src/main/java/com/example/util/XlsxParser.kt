package com.example.util

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object XlsxParser {
    fun parse(inputStream: InputStream): List<List<String>> {
        val zipInputStream = ZipInputStream(BufferedInputStream(inputStream, 16 * 1024))
        var sharedStrings: List<String> = emptyList()
        var sheetBytes: ByteArray? = null

        var entry = zipInputStream.nextEntry
        while (entry != null) {
            when (entry.name) {
                "xl/sharedStrings.xml", "/xl/sharedStrings.xml" -> {
                    sharedStrings = parseSharedStrings(zipInputStream)
                }
                "xl/worksheets/sheet1.xml", "/xl/worksheets/sheet1.xml" -> {
                    sheetBytes = zipInputStream.readBytes()
                }
            }
            zipInputStream.closeEntry()
            if (sharedStrings.isNotEmpty() && sheetBytes != null) break
            entry = zipInputStream.nextEntry
        }

        return if (sheetBytes != null) {
            parseWorksheet(sheetBytes.inputStream(), sharedStrings)
        } else {
            emptyList()
        }
    }

    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val strings = ArrayList<String>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType
        var currentSiText: StringBuilder? = null
        var inT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> currentSiText = StringBuilder()
                        "t" -> if (currentSiText != null) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inT) currentSiText?.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> inT = false
                        "si" -> {
                            strings.add(currentSiText?.toString() ?: "")
                            currentSiText = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseWorksheet(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = ArrayList<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType

        val currentCells = HashMap<Int, String>(8)
        var maxRowCol = 0
        var currentColIdx = 0
        var currentCellType: String? = null
        var inValue = false
        val currentText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentCells.clear()
                            maxRowCol = 0
                        }
                        "c" -> {
                            currentColIdx = excelRefToColIndex(parser.getAttributeValue(null, "r"))
                            currentCellType = parser.getAttributeValue(null, "t")
                            currentText.setLength(0)
                        }
                        "v" -> inValue = true
                        "t" -> if (currentCellType == "inlineStr") inValue = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue) currentText.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inValue = false
                        "t" -> if (currentCellType == "inlineStr") inValue = false
                        "c" -> {
                            if (currentColIdx > 0) {
                                val rawValue = currentText.toString()
                                val cellValue = if (currentCellType == "s") {
                                    val idx = rawValue.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                                } else {
                                    rawValue
                                }.trim()
                                currentCells[currentColIdx] = cellValue
                                if (currentColIdx > maxRowCol) maxRowCol = currentColIdx
                            }
                            currentColIdx = 0
                            currentCellType = null
                        }
                        "row" -> {
                            if (maxRowCol > 0) {
                                val rowList = ArrayList<String>(maxRowCol)
                                for (i in 1..maxRowCol) {
                                    rowList.add(currentCells[i] ?: "")
                                }
                                while (rowList.isNotEmpty() && rowList.last().isEmpty()) {
                                    rowList.removeAt(rowList.lastIndex)
                                }
                                if (rowList.isNotEmpty()) {
                                    rows.add(rowList)
                                }
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun excelRefToColIndex(ref: String?): Int {
        if (ref.isNullOrEmpty()) return 0
        var num = 0
        for (i in 0 until ref.length) {
            val c = ref[i]
            if (c in 'A'..'Z') {
                num = num * 26 + (c - 'A' + 1)
            } else if (c in 'a'..'z') {
                num = num * 26 + (c - 'a' + 1)
            } else {
                break
            }
        }
        return num
    }
}
