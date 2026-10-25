package com.example.paisapet.sms

/** What the generic rule found: the amount and the single 12-digit UPI reference. */
data class FallbackMatch(val amountPaise: Long, val upiRef: String)

/**
 * Last-chance capture for a UPI debit that no bank template matched. Every condition must hold;
 * a miss is always preferred to a wrong capture. Pure Kotlin, no Android classes, no logging.
 */
object GenericDebitFallback {

    private val UPI = Regex("""\bUPI\b""", RegexOption.IGNORE_CASE)
    private val DIGIT_RUN = Regex("""\d+""")
    private const val REF_LENGTH = 12

    // Whole phrases, any whitespace between words. The end is "not a letter" rather than \b so "sent ₹ 450" matches.
    private val DEBIT_PHRASE = Regex(
        listOf("debited from", "debited by", "debited for", "debited with", "sent rs", "sent inr", "sent ₹", "paid rs")
            .joinToString("|", prefix = """\b(?:""", postfix = """)(?![A-Za-z])""") { phrase ->
                phrase.split(" ").joinToString("""\s+""") { Regex.escape(it) }
            },
        RegexOption.IGNORE_CASE,
    )

    private val BLOCKED_WORDS = Regex(
        """\b(?:credited|credit|received|refund|reversed|reversal|ATM|NEFT|IMPS|RTGS|OTP|failed|declined|unsuccessful|pending)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val CARD = Regex("card", RegexOption.IGNORE_CASE)

    private const val AMOUNT = """(\d[\d,]*(?:\.\d{1,2})?)"""

    // Group 1: after Rs / Rs. / INR / ₹. Group 2: right after "debited by" (SBI style, no currency).
    private val AMOUNT_CANDIDATE = Regex(
        """(?:\b(?:Rs\.?|INR)|₹)\s*$AMOUNT|\bdebited\s+by\s+$AMOUNT""",
        RegexOption.IGNORE_CASE,
    )
    private val BALANCE_HINT = Regex("bal|avl|available", RegexOption.IGNORE_CASE)
    private const val BALANCE_LOOKBACK = 12

    private val BANK_SENDER = Regex("""^\p{L}\S{3,}$""")

    fun match(sender: String, body: String): FallbackMatch? {
        if (!BANK_SENDER.matches(sender.trim())) return null
        if (!UPI.containsMatchIn(body)) return null
        if (!DEBIT_PHRASE.containsMatchIn(body)) return null
        if (BLOCKED_WORDS.containsMatchIn(body) || CARD.containsMatchIn(body)) return null
        val ref = singleRef(body) ?: return null
        val amountPaise = firstAmount(body) ?: return null
        return FallbackMatch(amountPaise, ref)
    }

    /** The one distinct 12-digit run, ignoring "+91…" phone numbers; null for none or several. */
    private fun singleRef(body: String): String? {
        val refs = DIGIT_RUN.findAll(body)
            .filter { it.value.length == REF_LENGTH }
            .filterNot { it.value.startsWith("91") && it.range.first > 0 && body[it.range.first - 1] == '+' }
            .map { it.value }
            .toSet()
        return refs.singleOrNull()
    }

    /** The first amount not preceded within 12 characters by a balance word; null if none or not > 0. */
    private fun firstAmount(body: String): Long? {
        val first = AMOUNT_CANDIDATE.findAll(body).firstOrNull { match ->
            val before = body.substring(maxOf(0, match.range.first - BALANCE_LOOKBACK), match.range.first)
            !BALANCE_HINT.containsMatchIn(before)
        } ?: return null
        val text = first.groups[1]?.value ?: first.groups[2]?.value ?: return null
        return SmsFields.amountToPaise(text)?.takeIf { it > 0 }
    }
}
