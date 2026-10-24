package com.example.paisapet.sms

import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

enum class Expect { DEBIT, IGNORE, DUPLICATE }

/** One `### ID` block of docs/sample-sms.txt. Only [body] is a real message; the rest is test metadata. */
data class FixtureBlock(
    val id: String,
    val sender: String,
    val expect: Expect,
    val amountPaise: Long? = null,
    val merchant: String? = null,
    val date: LocalDate? = null,
    val month: YearMonth? = null,
    val ref: String? = null,
    val receivedAt: Instant? = null,
    val reason: String? = null,
    val duplicateOf: String? = null,
    val synthetic: Boolean = false,
    val note: String? = null,
    val body: String,
)

/** Reads the fixture file format described in the header of docs/sample-sms.txt. */
object SmsFixtures {

    /** Unit tests run from the app/ folder. */
    const val PATH = "../docs/sample-sms.txt"

    private val KNOWN_KEYS = setOf(
        "sender", "expect", "amountPaise", "merchant", "date", "month", "ref",
        "receivedAt", "reason", "duplicateOf", "synthetic", "note",
    )

    fun load(): List<FixtureBlock> {
        val file = File(PATH)
        check(file.isFile) { "Fixture file not found: ${file.absolutePath}" }
        return parse(file.readText(Charsets.UTF_8))
    }

    fun parse(text: String): List<FixtureBlock> {
        val blocks = mutableListOf<FixtureBlock>()
        var id: String? = null
        var fields = mutableMapOf<String, String>()
        var bodyLines: MutableList<String>? = null

        for (line in text.lines()) {
            val body = bodyLines
            if (body != null) {
                if (line == "---") {
                    blocks += build(id!!, fields, body.joinToString("\n"))
                    id = null
                    fields = mutableMapOf()
                    bodyLines = null
                } else {
                    body += line
                }
                continue
            }
            when {
                line.startsWith("### ") -> {
                    require(id == null) { "Block $id: no body before the next block starts" }
                    id = line.removePrefix("### ").trim()
                }
                line.isBlank() || line.startsWith("#") -> Unit
                id == null -> throw IllegalArgumentException("Text outside a block: \"$line\"")
                line == "body:" -> bodyLines = mutableListOf()
                else -> {
                    val sep = line.indexOf(": ")
                    require(sep > 0) { "Block $id: expected \"key: value\" but got \"$line\"" }
                    val key = line.substring(0, sep)
                    require(key in KNOWN_KEYS) { "Block $id: unknown key \"$key\"" }
                    require(key !in fields) { "Block $id: key \"$key\" given twice" }
                    fields[key] = line.substring(sep + 2)
                }
            }
        }
        require(id == null && bodyLines == null) { "Block $id: not closed with ---" }
        return blocks
    }

    private fun build(id: String, f: Map<String, String>, body: String): FixtureBlock {
        fun <T> typed(key: String, convert: (String) -> T): T? =
            f[key]?.let {
                try {
                    convert(it)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Block $id: bad $key \"$it\"", e)
                }
            }
        return FixtureBlock(
            id = id,
            sender = f["sender"] ?: throw IllegalArgumentException("Block $id: missing sender"),
            expect = typed("expect") { Expect.valueOf(it) }
                ?: throw IllegalArgumentException("Block $id: missing expect"),
            amountPaise = typed("amountPaise") { it.toLong() },
            merchant = f["merchant"],
            date = typed("date") { LocalDate.parse(it) },
            month = typed("month") { YearMonth.parse(it) },
            ref = f["ref"],
            receivedAt = typed("receivedAt") { OffsetDateTime.parse(it).toInstant() },
            reason = f["reason"],
            duplicateOf = f["duplicateOf"],
            synthetic = typed("synthetic") { it.toBooleanStrict() } ?: false,
            note = f["note"],
            body = body,
        )
    }
}
