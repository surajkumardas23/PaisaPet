package com.example.paisapet.debugtools

import com.example.paisapet.sms.ParseResult
import com.example.paisapet.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

class SampleSmsTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 10, 25)
    private val receivedAt = today.atTime(LocalTime.NOON).atZone(zone).toInstant()

    private fun checkBank(bank: SampleBank) {
        val random = Random(seed = bank.ordinal + 1)
        val refs = mutableSetOf<String>()
        repeat(50) { n ->
            val sample = generate(bank, random, today)
            val result = SmsParser.parse(sample.sender, sample.body, receivedAt, zone)
            assertTrue("$bank #$n: expected Debit but got $result", result is ParseResult.Debit)
            val debit = (result as ParseResult.Debit).debit
            assertEquals("$bank #$n amount", sample.amountPaise, debit.amountPaise)
            assertTrue("$bank #$n amount range", sample.amountPaise in 1_000..50_000)
            assertEquals("$bank #$n ref", sample.ref, debit.upiRef)
            assertTrue("$bank #$n ref is 12 digits", debit.upiRef!!.matches(Regex("\\d{12}")))
            assertEquals("$bank #$n month", "2026-10", debit.monthKey)
            refs += sample.ref
        }
        assertEquals("$bank refs all different", 50, refs.size)
    }

    @Test fun hdfcSamplesParse() = checkBank(SampleBank.HDFC)

    @Test fun sbiSamplesParse() = checkBank(SampleBank.SBI)

    @Test fun iciciSamplesParse() = checkBank(SampleBank.ICICI)

    @Test fun lineBreaksMatchTheRealFixtures() {
        val random = Random(7)
        assertEquals(7, generate(SampleBank.HDFC, random, today).body.lines().size)
        assertEquals(1, generate(SampleBank.SBI, random, today).body.lines().size)
        assertEquals(1, generate(SampleBank.ICICI, random, today).body.lines().size)
    }

    @Test fun datesInEachBanksFormat() {
        val random = Random(7)
        assertTrue(generate(SampleBank.HDFC, random, today).body.contains("\n25/10/26\n"))
        assertTrue(generate(SampleBank.SBI, random, today).body.contains("on date 25Oct26 trf to"))
        assertTrue(generate(SampleBank.ICICI, random, today).body.contains(" on 25-Oct-26; "))
    }
}
