package com.example.paisapet.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GenericDebitFallbackTest {

    private val sender = "AX-AXISBK-S"
    private val axis = "AXISBK: INR 450.00 debited from A/c XX1111 on 25-10-2026 via UPI Ref 123456789012. Not you? Call 1800"
    private val expected = FallbackMatch(45000, "123456789012")

    private fun match(body: String, from: String = sender) = GenericDebitFallback.match(from, body)

    // Captured

    @Test fun unknownBankDebit() = assertEquals(expected, match(axis))

    @Test fun lowercase() = assertEquals(expected, match(axis.lowercase()))

    @Test fun lineBreaksBetweenWords() = assertEquals(expected, match(axis.replace(" ", "\n")))

    @Test fun amountAfterDebitedBy() =
        assertEquals(
            FallbackMatch(4900, "123456789012"),
            match("Dear customer A/c X1111 debited by 49.00 on 25Oct26 via UPI Ref 123456789012"),
        )

    @Test fun everyDebitPhrase() {
        val phrases = listOf(
            "debited from A/c X1 Rs 10.00", "debited by Rs 10.00", "debited for Rs 10.00", "debited with Rs 10.00",
            "Sent Rs.10.00", "sent INR 10.00", "Sent ₹10.00", "Sent ₹ 10.00", "paid Rs 10.00",
        )
        for (p in phrases) assertEquals(p, FallbackMatch(1000, "123456789012"), match("$p via UPI Ref 123456789012"))
    }

    @Test fun phoneNumberWithCountryCodeIsNotARef() =
        assertEquals(expected, match("$axis or +919876543210"))

    @Test fun sameRefTwiceIsOneRef() = assertEquals(expected, match("$axis Ref again 123456789012"))

    @Test fun balanceAmountSkippedForTheRealOne() =
        assertEquals(expected, match("Avl Bal Rs 5000.00. $axis"))

    // Not captured

    @Test fun creditedTo() = assertNull(match(axis.replace("debited from", "credited to")))

    @Test fun creditWording() =
        assertNull(match("INR 450.00 credited to A/c XX1111 on 25-10-2026 via UPI Ref 123456789012"))

    @Test fun blockedWords() {
        for (word in listOf("Card", "ATM", "NEFT", "IMPS", "RTGS", "refund", "received", "OTP", "failed",
            "declined", "unsuccessful", "pending", "credit", "reversed", "reversal")) {
            assertNull(word, match("$axis $word"))
        }
    }

    @Test fun cardAsSubstring() = assertNull(match("$axis SBIDrCard"))

    @Test fun noUpiWord() = assertNull(match(axis.replace("UPI ", "")))

    @Test fun upiOnlyInsideAnotherWord() = assertNull(match(axis.replace("UPI ", "UPIX ")))

    @Test fun elevenDigitRef() = assertNull(match(axis.replace("123456789012", "12345678901")))

    @Test fun thirteenDigitNumberOnly() = assertNull(match(axis.replace("123456789012", "1234567890123")))

    @Test fun twoDifferentRefs() = assertNull(match("$axis Other ref 210987654321"))

    @Test fun debitedWithoutAnExplicitPhrase() =
        assertNull(match("Amount debited 450 via UPI Ref 123456789012", from = sender))

    @Test fun onlyABalanceAmount() =
        assertNull(match("Debited from A/c XX1111. Avl Bal Rs 5000 UPI Ref 123456789012"))

    @Test fun zeroAmount() = assertNull(match(axis.replace("450.00", "0.00")))

    @Test fun senderMustLookLikeABank() {
        assertEquals(expected, match(axis, from = "AD-HDFCBK-S"))
        assertEquals(expected, match(axis, from = "VK-SBIUPI-S"))
        assertEquals(expected, match(axis, from = "HDFCBK"))
        assertEquals(expected, match(axis, from = " AXIS "))
        for (bad in listOf("+919876543210", "9876543210", "Mom", "AX1", "AX IS BK", "", "1AXISBK")) {
            assertNull(bad, match(axis, from = bad))
        }
    }
}
