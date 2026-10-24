package com.example.paisapet.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SmsFieldsTest {

    // Amounts

    @Test
    fun amount_noDecimals() = assertEquals(500000L, SmsFields.amountToPaise("5000"))

    @Test
    fun amount_indianCommas() = assertEquals(1250000L, SmsFields.amountToPaise("12,500.00"))

    @Test
    fun amount_nonZeroPaise() = assertEquals(77663L, SmsFields.amountToPaise("776.63"))

    @Test
    fun amount_oneDecimalDigitMeansTens() = assertEquals(19900L, SmsFields.amountToPaise("199.0"))

    @Test
    fun amount_onlyPaise() = assertEquals(50L, SmsFields.amountToPaise(".5"))

    @Test
    fun amount_lakhs() = assertEquals(11026726L, SmsFields.amountToPaise("1,10,267.26"))

    @Test
    fun amount_zero() = assertEquals(0L, SmsFields.amountToPaise("0.00"))

    @Test
    fun amount_rejectsNonAmounts() {
        assertNull(SmsFields.amountToPaise(""))
        assertNull(SmsFields.amountToPaise("."))
        assertNull(SmsFields.amountToPaise("12.345"))
        assertNull(SmsFields.amountToPaise("Rs.12"))
        assertNull(SmsFields.amountToPaise("99999999999999999999"))
    }

    // Dates (day first)

    @Test
    fun date_hdfcSlashes() = assertEquals(LocalDate.of(2026, 10, 3), SmsFields.readMessageDate("03/10/26"))

    @Test
    fun date_sbiCompact() = assertEquals(LocalDate.of(2026, 9, 28), SmsFields.readMessageDate("28Sep26"))

    @Test
    fun date_sbiCompactLowercase() = assertEquals(LocalDate.of(2026, 9, 28), SmsFields.readMessageDate("28sep26"))

    @Test
    fun date_iciciDashes() = assertEquals(LocalDate.of(2026, 6, 28), SmsFields.readMessageDate("28-Jun-26"))

    @Test
    fun date_twoDigitYearIs2000Plus() {
        assertEquals(LocalDate.of(2099, 1, 5), SmsFields.readMessageDate("05/01/99"))
        assertEquals(LocalDate.of(2000, 1, 5), SmsFields.readMessageDate("05/01/00"))
    }

    @Test
    fun date_impossibleDateIsNull() {
        assertNull(SmsFields.readMessageDate("31/02/26"))
        assertNull(SmsFields.readMessageDate("30Feb26"))
    }

    @Test
    fun date_garbageIsNull() = assertNull(SmsFields.readMessageDate("yesterday"))

    // Bank from sender

    @Test
    fun bank_stripsPrefixAndSuffix() {
        assertEquals("HDFCBK", SmsFields.bankFromSender("AD-HDFCBK-S"))
        assertEquals("SBIUPI", SmsFields.bankFromSender("VK-SBIUPI-S"))
        assertEquals("ICICIT", SmsFields.bankFromSender("JD-ICICIT-S"))
    }

    @Test
    fun bank_prefixOnly() = assertEquals("HDFCBK", SmsFields.bankFromSender("VM-HDFCBK"))

    @Test
    fun bank_bareId() = assertEquals("HDFCBK", SmsFields.bankFromSender("HDFCBK"))

    @Test
    fun bank_phoneNumberIsNull() = assertNull(SmsFields.bankFromSender("+919876500123"))

    // Phone-number senders

    @Test
    fun phone_detected() {
        assertTrue(SmsFields.isPhoneNumberSender("+919876500123"))
        assertTrue(SmsFields.isPhoneNumberSender("9876500123"))
        assertTrue(SmsFields.isPhoneNumberSender("+91 98765 00123"))
    }

    @Test
    fun phone_bankIdsAreNotPhones() {
        assertFalse(SmsFields.isPhoneNumberSender("AD-HDFCBK-S"))
        assertFalse(SmsFields.isPhoneNumberSender("57575"))
    }

    // smsHash

    @Test
    fun hash_knownValue() = assertEquals(
        // printf 'AD-HDFCBK-S\nhello' | sha256sum
        "cd2cf9c25233bb4d032fdb7fbaacac7390f2c63c65c4ff3dd36eeb13f22fd940",
        smsHash("AD-HDFCBK-S", "hello"),
    )

    @Test
    fun hash_isStableAnd64Hex() {
        val hash = smsHash("JD-ICICIT-S", "body")
        assertEquals(hash, smsHash("JD-ICICIT-S", "body"))
        assertTrue(hash.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun hash_separatorPreventsCollision() = assertNotEquals(smsHash("AB", "C"), smsHash("A", "BC"))
}
