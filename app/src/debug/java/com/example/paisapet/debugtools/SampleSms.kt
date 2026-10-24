package com.example.paisapet.debugtools

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

enum class SampleBank { HDFC, SBI, ICICI }

/** A made-up bank SMS for Simulate SMS, plus what the parser should find in it. */
data class SampleSms(val sender: String, val body: String, val amountPaise: Long, val ref: String)

private val PAYEES = listOf("SWIGGY", "ZEPTO MARKETPLAC", "TEA POST", "URBANCOMPANY")

private val HDFC_DATE = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH)
private val SBI_DATE = DateTimeFormatter.ofPattern("ddMMMyy", Locale.ENGLISH)
private val ICICI_DATE = DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH)

/**
 * A fresh sample in the bank's real wording (docs/sample-sms.txt: HDFC D01, SBI D02/D06, ICICI D03/D04),
 * dated [today], with a random 12-digit ref, a random ₹10.00–₹500.00 amount and a random payee.
 */
fun generate(bank: SampleBank, random: Random, today: LocalDate): SampleSms {
    val amountPaise = random.nextLong(1_000, 50_001)
    val amount = String.format(Locale.ROOT, "%d.%02d", amountPaise / 100, amountPaise % 100)
    val ref = random.nextLong(100_000_000_000, 1_000_000_000_000).toString()
    val payee = PAYEES[random.nextInt(PAYEES.size)]
    return when (bank) {
        SampleBank.HDFC -> SampleSms(
            sender = "AD-HDFCBK-S",
            body = listOf(
                "UPI Mandate:",
                "Sent Rs.$amount",
                "from HDFC Bank A/c 4821",
                "To $payee",
                today.format(HDFC_DATE),
                "Ref $ref",
                "Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
            ).joinToString("\n"),
            amountPaise = amountPaise,
            ref = ref,
        )
        SampleBank.SBI -> SampleSms(
            sender = "VK-SBIUPI-S",
            body = "Dear UPI user A/C X3190 debited by $amount on date ${today.format(SBI_DATE)} trf to $payee " +
                "Refno $ref If not u? call-1800111109 for other services-18001234-SBI",
            amountPaise = amountPaise,
            ref = ref,
        )
        SampleBank.ICICI -> SampleSms(
            sender = "JD-ICICIT-S",
            body = "ICICI Bank Acct XX518 debited for Rs $amount on ${today.format(ICICI_DATE)}; $payee credited. " +
                "UPI:$ref. Call 18002662 for dispute. SMS BLOCK 518 to 9215676766.",
            amountPaise = amountPaise,
            ref = ref,
        )
    }
}
