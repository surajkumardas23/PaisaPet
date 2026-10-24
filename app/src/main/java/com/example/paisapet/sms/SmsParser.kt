package com.example.paisapet.sms

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Turns one SMS into a UPI debit or an ignore reason. Pure Kotlin, no Android classes,
 * no logging. A message is a debit only if one bank template in [DebitTemplates] matches it.
 *
 * Order: (1) sender check, (2) hard-reject phrases, (3) bank templates, (4) explain the ignore.
 */
object SmsParser {

    private const val MAX_PAYEE_LENGTH = 40
    private const val NO_PAYEE = "UPI payment"

    private val UPI_WORD = word("UPI")
    private val LINE_BREAK = Regex("""\r\n|\r|\n""")

    // (2) Hard rejects: whole phrases only. Single words such as "mandate", "credited",
    // "failed" or "declined" appear inside real debits (D01, D03) and never reject on their own.
    private val COLLECT_REQUEST = phrases("collect request")
    private val MANDATE_SETUP = phrases("created a mandate", "mandate created", "mandate registered", "registered a mandate")
    private val PRE_DEBIT_NOTICE = phrases("will be debited")
    private val OTP_CODE = Regex("""\bOTP\b[:\-\s]*\d{4,8}\b""", RegexOption.IGNORE_CASE)
    private val ONE_TIME_PASSWORD = phrases("one time password")
    private val WILL_BE_DELIVERED = phrases("will be delivered")
    private val FAILED = phrases(
        "has failed", "payment failed", "transaction failed", "txn failed",
        "is unsuccessful", "was unsuccessful", "not processed",
    )
    private val DECLINED = phrases(
        "txn declined", "transaction declined", "payment declined", "has been declined", "was declined",
    )

    // (4) Explain rules. Labels only; they run after the templates failed.
    private val CARD = Regex("card", RegexOption.IGNORE_CASE) // substring: SBI writes "SBIDrCard"
    private val REVERSED = word("reversed")
    private val SPENT_OR_USED = Regex("""\b(?:spent|used)\b""", RegexOption.IGNORE_CASE)
    private val CREDITED_TO_CARD = phrases("credited to your card")
    private val LOAN = word("loan")
    private val CREDITED = word("credited")
    private val IMPS = word("IMPS")
    private val BALANCE = Regex("""\b(?:Available|Avl|Avail)\s+Bal""", RegexOption.IGNORE_CASE)

    fun parse(sender: String, body: String, receivedAt: Instant, zone: ZoneId): ParseResult {
        if (SmsFields.isPhoneNumberSender(sender)) return ParseResult.Ignored(IgnoreReason.SENDER_IS_PHONE_NUMBER)

        hardRejectReason(body)?.let { return ParseResult.Ignored(it) }

        if (UPI_WORD.containsMatchIn(body)) {
            for (template in DebitTemplates.ALL) {
                val match = template.regex.find(body) ?: continue
                return toDebit(template, match.groupValues, sender, body, receivedAt, zone)
            }
        }
        return ParseResult.Ignored(explainIgnore(body))
    }

    private fun hardRejectReason(body: String): IgnoreReason? = when {
        COLLECT_REQUEST.containsMatchIn(body) -> IgnoreReason.COLLECT_REQUEST
        MANDATE_SETUP.containsMatchIn(body) -> IgnoreReason.MANDATE_SETUP
        PRE_DEBIT_NOTICE.containsMatchIn(body) -> IgnoreReason.MANDATE_PRE_DEBIT_NOTICE
        OTP_CODE.containsMatchIn(body) ->
            if (WILL_BE_DELIVERED.containsMatchIn(body)) IgnoreReason.OTP_DELIVERY else IgnoreReason.OTP
        ONE_TIME_PASSWORD.containsMatchIn(body) -> IgnoreReason.OTP
        FAILED.containsMatchIn(body) -> IgnoreReason.FAILED_PAYMENT
        DECLINED.containsMatchIn(body) ->
            if (CARD.containsMatchIn(body)) IgnoreReason.CARD_DECLINED else IgnoreReason.FAILED_PAYMENT
        else -> null
    }

    private fun explainIgnore(body: String): IgnoreReason {
        fun has(regex: Regex) = regex.containsMatchIn(body)
        return when {
            has(CARD) && has(REVERSED) -> IgnoreReason.CARD_REVERSAL
            has(CARD) && has(SPENT_OR_USED) -> IgnoreReason.CARD_SPEND
            has(CREDITED_TO_CARD) -> IgnoreReason.CREDIT_CARD_PAYMENT
            has(LOAN) && has(CREDITED) -> IgnoreReason.CREDIT_LOAN_ACCOUNT
            has(CREDITED) && has(IMPS) -> IgnoreReason.CREDIT_IMPS
            has(CREDITED) && has(UPI_WORD) -> IgnoreReason.CREDIT_UPI
            has(BALANCE) -> IgnoreReason.BALANCE_ALERT
            else -> IgnoreReason.UNRECOGNISED
        }
    }

    private fun toDebit(
        template: DebitTemplate,
        groups: List<String>,
        sender: String,
        body: String,
        receivedAt: Instant,
        zone: ZoneId,
    ): ParseResult {
        val amountPaise = SmsFields.amountToPaise(groups[template.amountGroup])
        if (amountPaise == null || amountPaise <= 0) return ParseResult.Ignored(IgnoreReason.NO_AMOUNT)

        val receivedLocal = receivedAt.atZone(zone)
        val messageDate = SmsFields.readMessageDate(groups[template.dateGroup])
        val occurredAt = messageDate?.atTime(receivedLocal.toLocalTime())?.atZone(zone)?.toInstant() ?: receivedAt

        return ParseResult.Debit(
            ParsedDebit(
                amountPaise = amountPaise,
                payee = cleanPayee(groups[template.payeeGroup]),
                upiRef = groups[template.refGroup],
                bank = SmsFields.bankFromSender(sender),
                occurredAt = occurredAt,
                monthKey = YearMonth.from(occurredAt.atZone(zone)).toString(),
                dateFromMessage = messageDate != null,
                smsHash = smsHash(sender, body),
            ),
        )
    }

    /** Keeps inner spacing as printed; a line break becomes one space; trims the ends; caps the length. */
    private fun cleanPayee(raw: String): String {
        val payee = raw.replace(LINE_BREAK, " ").trim().take(MAX_PAYEE_LENGTH).trim()
        return payee.ifEmpty { NO_PAYEE }
    }

    private fun word(w: String) = Regex("""\b${Regex.escape(w)}\b""", RegexOption.IGNORE_CASE)

    /** Whole phrases, case-insensitive, any whitespace (including line breaks) between words. */
    private fun phrases(vararg list: String): Regex {
        val alternatives = list.joinToString("|") { phrase ->
            phrase.split(" ").joinToString("""\s+""") { Regex.escape(it) }
        }
        return Regex("""\b(?:$alternatives)\b""", RegexOption.IGNORE_CASE)
    }
}
