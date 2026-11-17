package com.example.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

object SampleFileGenerator {
    private const val PREF_KEY = "sample_file_created_v1"
    private const val SAMPLE_FILE_NAME = "sample_flashcards.csv"

    private val SAMPLE_CSV_CONTENT = """
        食べる,たべる,to eat,ichidan verb
        飲む,のむ,to drink,
        こんにちは,,Hello,polite greeting
        水,,Water,
        電車,でんしゃ,Train,noun
        本,,Book,
    """.trimIndent() + "\n"

    fun ensureSampleFileExists(context: Context) {
        val prefs = context.getSharedPreferences("flashcards_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_KEY, false)) return

        try {
            val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeViaMediaStore(context)
            } else {
                writeToAppDownloads(context)
            }
            if (created) {
                prefs.edit().putBoolean(PREF_KEY, true).apply()
            }
        } catch (e: Exception) {
            Log.e("SampleFileGenerator", "Failed to create sample flashcard file", e)
        }
    }

    private fun writeViaMediaStore(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val resolver = context.contentResolver

        val existingProjection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(SAMPLE_FILE_NAME)
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            existingProjection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return true
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, SAMPLE_FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            out.write(SAMPLE_CSV_CONTENT.toByteArray(Charsets.UTF_8))
        }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return true
    }

    private fun writeToAppDownloads(context: Context): Boolean {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val file = File(dir, SAMPLE_FILE_NAME)
        if (!file.exists()) {
            file.writeText(SAMPLE_CSV_CONTENT, Charsets.UTF_8)
        }
        return true
    }
}
