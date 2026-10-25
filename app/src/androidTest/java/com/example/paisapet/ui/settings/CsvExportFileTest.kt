package com.example.paisapet.ui.settings

import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.export.CSV_HEADER
import com.example.paisapet.export.UTF8_BOM
import com.example.paisapet.export.writeExportFile
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The export file through the app's real cache folder and FileProvider. Runs on the phone with connectedAndroidTest. */
@RunWith(AndroidJUnit4::class)
class CsvExportFileTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val ist: ZoneId = ZoneId.of("Asia/Kolkata")

    private val row = LocalDateTime.of(2026, 10, 25, 13, 15).atZone(ist).toInstant().toEpochMilli().let { at ->
        TransactionEntity(
            id = 1, amountPaise = 240_00, payee = "Swiggy", upiRef = null, bank = "HDFCBK", occurredAt = at,
            monthKey = "2026-10", category = Category.MISC, source = Source.SMS, tagSource = null, taggedAt = null,
            smsHash = "h1", createdAt = at, updatedAt = at,
        )
    }

    @Test fun exportStartsWithBomAndHeader_andIsSharedAsContentUri() {
        val file = writeExportFile(exportDir(context), listOf(row), ist, LocalDate.of(2026, 10, 25))
        try {
            assertEquals(exportDir(context), file.parentFile)
            val bytes = file.readBytes()
            assertArrayEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), bytes.copyOfRange(0, 3))
            assertTrue(String(bytes, Charsets.UTF_8).removePrefix(UTF8_BOM).startsWith(CSV_HEADER + "\r\n"))

            val uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), file)
            assertEquals("content", uri.scheme)
            assertEquals(fileProviderAuthority(context), uri.authority)
        } finally {
            file.delete()
        }
    }
}
