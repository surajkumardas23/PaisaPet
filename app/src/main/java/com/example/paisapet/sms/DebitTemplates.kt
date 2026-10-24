package com.example.paisapet.sms

/**
 * One bank's UPI debit wording. The regex is matched case-insensitively with `find`, and every
 * gap between words is `\s+`, so extra spaces and line breaks still match. The group numbers say
 * where each field is, because the banks print them in different orders.
 */
internal class DebitTemplate(
    val name: String,
    pattern: String,
    val amountGroup: Int,
    val payeeGroup: Int,
    val dateGroup: Int,
    val refGroup: Int,
) {
    val regex = Regex(pattern, RegexOption.IGNORE_CASE)
}

internal object DebitTemplates {

    private const val AMOUNT = """(\d[\d,]*(?:\.\d{1,2})?)"""
    private const val CURRENCY = """(?:Rs\.?|INR|₹)\s?"""

    // The payee may span a line break, so it is [\s\S] rather than '.'.
    private const val PAYEE = """([\s\S]+?)"""

    /**
     * AutoPay: "UPI Mandate:\nSent Rs.304.00\nfrom HDFC Bank A/c 4821\nTo RentomojoPvtLtd\n03/10/26\nRef 538204917365".
     * Regular: "Sent Rs.312.74\nFrom HDFC Bank A/C *4821\nTo Zomato Media Private Limi\nOn 25/10/26\nRef 534902716384".
     * The optional "On" must be a whole word after whitespace, so a payee such as "Amazon" keeps its "on".
     */
    val HDFC = DebitTemplate(
        name = "HDFC",
        pattern = """(?:UPI\s+Mandate:\s+)?Sent\s+$CURRENCY$AMOUNT\s+from\s+HDFC\s+Bank\s+A/c\s+\S+\s+To\s+$PAYEE\s+(?:On\s+)?(\d{1,2}/\d{1,2}/\d{2})\s+Ref\s+(\d{12})""",
        amountGroup = 1, payeeGroup = 2, dateGroup = 3, refGroup = 4,
    )

    /** "debited by 49.00 on date 28Sep26 trf to Jio Prepaid Rech Refno 704219356618". No Rs/INR. */
    val SBI = DebitTemplate(
        name = "SBI",
        pattern = """debited\s+by\s+$AMOUNT\s+on\s+date\s+(\d{1,2}[A-Za-z]{3}\d{2})\s+trf\s+to\s+$PAYEE\s+Refno\s+(\d{12})""",
        amountGroup = 1, dateGroup = 2, payeeGroup = 3, refGroup = 4,
    )

    /**
     * "Acct XX518 debited for Rs 620.00 on 27-Jun-26; BIRRA BEVERAGES credited. UPI:651196203847".
     * "credited" here belongs to the payee after the ';'. The account is the one debited.
     */
    val ICICI = DebitTemplate(
        name = "ICICI",
        pattern = """Acct\s+\S+\s+debited\s+for\s+$CURRENCY$AMOUNT\s+on\s+(\d{1,2}-[A-Za-z]{3}-\d{2});\s*$PAYEE\s+credited\.\s*UPI:\s*(\d{12})""",
        amountGroup = 1, dateGroup = 2, payeeGroup = 3, refGroup = 4,
    )

    val ALL = listOf(HDFC, SBI, ICICI)
}
