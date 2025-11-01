package com.blazingbanana.spendtracklite

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.blazingbanana.spendtracklite.data.Expense

object CsvExporter {

    private const val CSV_FILE_NAME = "expenses.csv"
    private const val RELATIVE_DIRECTORY = "SpendTrack"

    private val csvDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss",
        Locale.UK
    ).withZone(ZoneId.systemDefault())

    fun exportExpenses(context: Context, expenses: List<Expense>): Boolean {
        if (expenses.isEmpty()) return false

        val csvContent = buildCsv(expenses)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeWithMediaStore(context, csvContent)
            } else {
                writeLegacy(context, csvContent)
            }
        } catch (ioException: IOException) {
            false
        }
    }

    private fun writeWithMediaStore(context: Context, content: String): Boolean {
        val resolver = context.contentResolver
        val collection =
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val relativePath =
            Environment.DIRECTORY_DOCUMENTS + File.separator + RELATIVE_DIRECTORY + File.separator

        // Remove existing file with the same name so we can overwrite safely.
        resolver.delete(
            collection,
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(relativePath, CSV_FILE_NAME)
        )

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, CSV_FILE_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        val itemUri = resolver.insert(collection, values) ?: return false
        resolver.openOutputStream(itemUri, "w")?.use { outputStream ->
            outputStream.write(content.toByteArray(StandardCharsets.UTF_8))
        } ?: return false
        return true
    }

    @Suppress("DEPRECATION")
    private fun writeLegacy(context: Context, content: String): Boolean {
        val documentsFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val targetDirectory = File(documentsFolder, RELATIVE_DIRECTORY)
        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            return false
        }
        val targetFile = File(targetDirectory, CSV_FILE_NAME)
        FileOutputStream(targetFile, false).use { outputStream ->
            outputStream.write(content.toByteArray(StandardCharsets.UTF_8))
        }
        return true
    }

    private fun buildCsv(expenses: List<Expense>): String {
        val builder = StringBuilder()
        builder.append("date,amount,description\n")
        for (expense in expenses) {
            val dateText = csvDateFormatter.format(Instant.ofEpochMilli(expense.timestamp))
            val amountText = String.format(Locale.UK, "%.2f", expense.amount)
            val escapedDescription = expense.description.replace("\"", "\"\"")
            builder.append('"')
                .append(dateText)
                .append('"')
                .append(',')
                .append(amountText)
                .append(',')
                .append('"')
                .append(escapedDescription)
                .append('"')
                .append('\n')
        }
        return builder.toString()
    }
}
