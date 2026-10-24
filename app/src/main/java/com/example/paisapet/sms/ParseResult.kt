package com.example.paisapet.sms

import java.time.Instant

/** What [SmsParser] returns for one SMS. Never holds the raw body. */
sealed interface ParseResult {
    data class Debit(val debit: ParsedDebit) : ParseResult
    data class Ignored(val reason: IgnoreReason) : ParseResult
}

data class ParsedDebit(
    /** Always > 0. */
    val amountPaise: Long,
    /** As the bank printed it, ends trimmed, max 40 chars, "UPI payment" if blank. */
    val payee: String,
    /** 12-digit UPI reference, if present. */
    val upiRef: String?,
    /** Sender ID without prefix and suffix, e.g. HDFCBK. */
    val bank: String?,
    /** Date printed in the message with the time of day from the received time. */
    val occurredAt: Instant,
    /** YYYY-MM of [occurredAt] in the given zone. */
    val monthKey: String,
    /** False when no date could be read and [occurredAt] is the received time. */
    val dateFromMessage: Boolean,
    /** SHA-256 of sender + "\n" + body, see [smsHash]. */
    val smsHash: String,
)

/** Why an SMS produced no transaction. [code] matches the `reason:` lines in docs/sample-sms.txt. */
enum class IgnoreReason(val code: String) {
    SENDER_IS_PHONE_NUMBER("sender-is-phone-number"),
    COLLECT_REQUEST("collect-request"),
    MANDATE_SETUP("mandate-setup"),
    MANDATE_PRE_DEBIT_NOTICE("mandate-pre-debit-notice"),
    OTP("otp"),
    OTP_DELIVERY("otp-delivery"),
    FAILED_PAYMENT("failed-payment"),
    CARD_DECLINED("card-declined"),
    CARD_SPEND("card-spend"),
    CARD_REVERSAL("card-reversal"),
    CREDIT_UPI("credit-upi"),
    CREDIT_IMPS("credit-imps"),
    CREDIT_CARD_PAYMENT("credit-card-payment"),
    CREDIT_LOAN_ACCOUNT("credit-loan-account"),
    BALANCE_ALERT("balance-alert"),
    NO_AMOUNT("no-amount"),
    UNRECOGNISED("unrecognised"),
}
