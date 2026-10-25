package com.example.paisapet.export

import com.example.paisapet.IST
import com.example.paisapet.tx
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class CsvFileTest {

    @get:Rule val tmp = TemporaryFolder()

    private val today = LocalDate.of(2026, 10, 25)

    @Test fun writesNamedFileWithBomAndCsv() {
        val dir = File(tmp.root, "exports")
        val rows = listOf(tx(payee = "Swiggy"))
        val file = writeExportFile(dir, rows, IST, today)
        assertEquals("paisa-pet-2026-10-25.csv", file.name)
        assertEquals(dir, file.parentFile)
        val bytes = file.readBytes()
        assertArrayEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), bytes.copyOfRange(0, 3))
        assertEquals(UTF8_BOM + buildCsv(rows, IST), file.readText(Charsets.UTF_8))
    }

    @Test fun createsTheFolderIfMissing() {
        val dir = File(tmp.root, "a/b/exports")
        assertFalse(dir.exists())
        writeExportFile(dir, emptyList(), IST, today)
        assertTrue(dir.isDirectory)
    }

    @Test fun deletesOlderExportsFirst() {
        val dir = tmp.newFolder("exports")
        File(dir, "paisa-pet-2026-09-01.csv").writeText("old")
        File(dir, "stray.tmp").writeText("x")
        val file = writeExportFile(dir, emptyList(), IST, today)
        assertEquals(listOf(file.name), dir.listFiles()!!.map { it.name })
    }

    @Test fun sameDayExportReplacesTheFile() {
        val dir = tmp.newFolder("exports")
        writeExportFile(dir, listOf(tx(id = 1)), IST, today)
        val second = writeExportFile(dir, emptyList(), IST, today)
        assertEquals(UTF8_BOM + CSV_HEADER + "\r\n", second.readText(Charsets.UTF_8))
        assertEquals(1, dir.listFiles()!!.size)
    }
}
