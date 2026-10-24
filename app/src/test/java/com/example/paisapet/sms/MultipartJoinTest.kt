package com.example.paisapet.sms

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MultipartJoinTest {

    @Test fun singlePart() {
        assertEquals(
            listOf(IncomingSms("VM-HDFCBK", "Sent Rs.10.00", Instant.ofEpochMilli(1000))),
            joinParts(listOf(SmsPart("VM-HDFCBK", "Sent Rs.10.00", 1000))),
        )
    }

    @Test fun partsFromOneSenderJoinInOrderWithEarliestTime() {
        val joined = joinParts(
            listOf(
                SmsPart("VM-HDFCBK", "Sent Rs.", 3000),
                SmsPart("VM-HDFCBK", "10.00 ", 1000),
                SmsPart("VM-HDFCBK", "To SWIGGY", 2000),
            ),
        )
        assertEquals(listOf(IncomingSms("VM-HDFCBK", "Sent Rs.10.00 To SWIGGY", Instant.ofEpochMilli(1000))), joined)
    }

    @Test fun interleavedSendersGroupInFirstSeenOrder() {
        val joined = joinParts(
            listOf(
                SmsPart("VK-SBIUPI", "a1", 10),
                SmsPart("VM-HDFCBK", "b1", 20),
                SmsPart("VK-SBIUPI", "a2", 30),
                SmsPart("VM-HDFCBK", "b2", 40),
            ),
        )
        assertEquals(
            listOf(
                IncomingSms("VK-SBIUPI", "a1a2", Instant.ofEpochMilli(10)),
                IncomingSms("VM-HDFCBK", "b1b2", Instant.ofEpochMilli(20)),
            ),
            joined,
        )
    }

    @Test fun emptyInput() = assertEquals(emptyList<IncomingSms>(), joinParts(emptyList()))

    @Test fun nullBodyBecomesEmpty() {
        assertEquals(
            listOf(IncomingSms("VM-HDFCBK", "ab", Instant.ofEpochMilli(5))),
            joinParts(listOf(SmsPart("VM-HDFCBK", "a", 5), SmsPart("VM-HDFCBK", null, 6), SmsPart("VM-HDFCBK", "b", 7))),
        )
    }

    @Test fun nullSenderBecomesEmpty() {
        assertEquals(
            listOf(IncomingSms("", "x", Instant.ofEpochMilli(5))),
            joinParts(listOf(SmsPart(null, "x", 5))),
        )
    }
}
