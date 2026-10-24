package com.example.paisapet.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Cases the fixture file does not cover. */
class SmsParserTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val receivedAt: Instant = Instant.parse("2026-06-27T06:30:00Z") // 12:00 IST

    private val iciciDebit =
        "ICICI Bank Acct XX518 debited for Rs 500.00 on 27-Jun-26; QUICK CASH credited. UPI:651196200001. " +
            "Call 18002662 for dispute. SMS BLOCK 518 to 9215676766."

    private fun parse(body: String, sender: String = "AD-HDFCBK-S") = SmsParser.parse(sender, body, receivedAt, zone)

    private fun assertIgnored(reason: IgnoreReason, result: ParseResult) =
        assertEquals(ParseResult.Ignored(reason), result)

    private fun assertDebit(result: ParseResult): ParsedDebit {
        assertTrue("expected Debit but got $result", result is ParseResult.Debit)
        return (result as ParseResult.Debit).debit
    }

    // (1) Sender check

    @Test
    fun sender_phoneWithPlusAndSpaces() =
        assertIgnored(IgnoreReason.SENDER_IS_PHONE_NUMBER, parse(iciciDebit, sender = "+91 98765 00123"))

    @Test
    fun sender_plainTenDigits() =
        assertIgnored(IgnoreReason.SENDER_IS_PHONE_NUMBER, parse(iciciDebit, sender = "9876500123"))

    @Test
    fun sender_nineDigitsIsNotAPhone() {
        assertFalse(SmsFields.isPhoneNumberSender("987650012"))
        assertDebit(parse(iciciDebit, sender = "987650012"))
    }

    @Test
    fun sender_bankIdIsNotAPhone() {
        assertFalse(SmsFields.isPhoneNumberSender("AD-HDFCBK-S"))
        assertDebit(parse(iciciDebit, sender = "AD-HDFCBK-S"))
    }

    // (2) Hard rejects without a fixture

    @Test
    fun mandateSetup_otherWordOrders() {
        for (body in listOf(
            "Your UPI mandate created for Rs 500.00 towards NETFLIX from HDFC Bank A/c 4821.",
            "UPI mandate registered for Rs 500.00 towards NETFLIX.",
            "You have registered a mandate of Rs 500.00 towards NETFLIX via UPI.",
        )) {
            assertIgnored(IgnoreReason.MANDATE_SETUP, parse(body))
        }
    }

    @Test
    fun otp_oneTimePassword() =
        assertIgnored(IgnoreReason.OTP, parse("Your one time password for UPI registration is 482913. Do not share."))

    @Test
    fun otp_withColon() =
        assertIgnored(IgnoreReason.OTP, parse("OTP: 123456 for your UPI transaction of Rs 500.00. Do not share."))

    @Test
    fun failedPayment_phrases() {
        for (body in listOf(
            "UPI payment failed for Rs 500.00 to SHOP.",
            "Your UPI transaction failed. Rs 500.00 was not debited.",
            "Your UPI payment of Rs 500.00 is unsuccessful.",
            "Your UPI request of Rs 500.00 was not processed.",
        )) {
            assertIgnored(IgnoreReason.FAILED_PAYMENT, parse(body))
        }
    }

    @Test
    fun declined_withCard() =
        assertIgnored(IgnoreReason.CARD_DECLINED, parse("Transaction declined on your Card xx1234 for Rs 100.00 at SHOP."))

    @Test
    fun declined_withoutCard() =
        assertIgnored(IgnoreReason.FAILED_PAYMENT, parse("Transaction declined for Rs 100.00 at SHOP via UPI."))

    // Single words never reject

    @Test
    fun creditedAndMandateWordsInRealDebits() {
        val fixtures = SmsFixtures.load()
        for (id in listOf("D03", "D01")) {
            val block = fixtures.single { it.id == id }
            assertDebit(SmsParser.parse(block.sender, block.body, receivedAt, zone))
        }
    }

    @Test
    fun failedAndDeclinedWordsInsideIciciDebit() {
        val body = "ICICI Bank Acct XX518 debited for Rs 100.00 on 27-Jun-26; FAILED DECLINED CAFE credited. " +
            "UPI:651196200002. Call 18002662 for dispute."
        val debit = assertDebit(parse(body, sender = "JD-ICICIT-S"))
        assertEquals("FAILED DECLINED CAFE", debit.payee)
    }

    // Currency escaping

    @Test
    fun hdfc_rsWithoutDotOrSpace() {
        val body = "Sent Rs304.00 from HDFC Bank A/c 4821 To SHOP 03/10/26 Ref 123456789012 " +
            "Not You? Call 18002586161/SMS BLOCK UPI to 7308080808"
        val debit = assertDebit(parse(body))
        assertEquals(30400L, debit.amountPaise)
        assertEquals("SHOP", debit.payee)
        assertEquals("123456789012", debit.upiRef)
    }

    // HDFC regular debit: "On" before the date, account written A/C *4821 (D10)

    private val hdfcRegular = "Sent Rs.312.74\nFrom HDFC Bank A/C *4821\nTo Zomato Media Private Limi\nOn 25/10/26\n" +
        "Ref 534902716384\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808"

    private fun assertZomato(debit: ParsedDebit) {
        assertEquals(31274L, debit.amountPaise)
        assertEquals("534902716384", debit.upiRef)
        assertEquals(LocalDate.of(2026, 10, 25), debit.occurredAt.atZone(zone).toLocalDate())
    }

    @Test
    fun hdfcRegular_payeeIntactWithoutOn() {
        val debit = assertDebit(parse(hdfcRegular))
        assertEquals("Zomato Media Private Limi", debit.payee)
        assertZomato(debit)
    }

    @Test
    fun hdfcRegular_allLowercase() {
        val debit = assertDebit(parse(hdfcRegular.lowercase()))
        assertEquals("zomato media private limi", debit.payee)
        assertZomato(debit)
    }

    @Test
    fun hdfcRegular_singleLine() {
        val body = "Sent Rs.312.74 From HDFC Bank A/C *4821 To Zomato Media Private Limi On 25/10/26 Ref 534902716384 " +
            "Not You? Call 18002586161/SMS BLOCK UPI to 7308080808"
        val debit = assertDebit(parse(body))
        assertEquals("Zomato Media Private Limi", debit.payee)
        assertZomato(debit)
    }

    @Test
    fun hdfcRegular_payeeEndingInOnKeepsIt() {
        val body = hdfcRegular.replace("Zomato Media Private Limi", "Amazon")
        assertEquals("Amazon", assertDebit(parse(body)).payee)
    }

    @Test
    fun hdfcAutoPay_withoutOnStillParses() {
        val body = "UPI Mandate:\nSent Rs.304.00\nfrom HDFC Bank A/c 4821\nTo RentomojoPvtLtd\n03/10/26\n" +
            "Ref 538204917365\nNot You? Call 18002586161/SMS BLOCK UPI to 7308080808"
        val debit = assertDebit(parse(body))
        assertEquals("RentomojoPvtLtd", debit.payee)
        assertEquals(30400L, debit.amountPaise)
        assertEquals("538204917365", debit.upiRef)
    }
}
