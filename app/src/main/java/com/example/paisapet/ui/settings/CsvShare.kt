package com.example.paisapet.ui.settings

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.paisapet.AppContainer
import com.example.paisapet.export.writeExportFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

// Thin Android glue for FR-18. The CSV itself is built and written by export/ (pure, tested).

/** The cache folder that file_paths.xml shares; nothing else in the app is reachable through the FileProvider. */
fun exportDir(context: Context): File = File(context.cacheDir, "exports")

fun fileProviderAuthority(context: Context): String = "${context.packageName}.fileprovider"

/**
 * Writes every payment to a CSV in the cache and opens the share sheet for it. Payment data travels only
 * inside the file, never as intent text. Returns false, without throwing, if anything fails.
 */
suspend fun shareCsvExport(context: Context, container: AppContainer, today: LocalDate): Boolean = try {
    val (file, uri) = withContext(Dispatchers.IO) {
        val file = writeExportFile(exportDir(context), container.repository.allForExport(), container.zone, today)
        file to FileProvider.getUriForFile(context, fileProviderAuthority(context), file)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/csv")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, file.name)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(file.name, uri)
    val chooser = Intent.createChooser(send, "Share ${file.name}")
    if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
    true
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    false
}
