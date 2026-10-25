package com.example.paisapet.notify

import com.example.paisapet.format.formatRupees
import com.example.paisapet.model.Category
import com.example.paisapet.sms.SmsParser

const val TAG_PROMPT_TEXT = "Misc or Essential spend? Tap to tag."

/** Shown on a secure lock screen instead of the amount and payee. */
const val TAG_PROMPT_PUBLIC_TEXT = "New UPI payment · Unlock to tag"

data class TagPromptText(val title: String, val text: String)

/** "₹240 paid to Swiggy", or "₹240 UPI payment" when the SMS named no payee. */
fun tagPromptText(amountPaise: Long, payee: String): TagPromptText {
    val amount = formatRupees(amountPaise)
    val title = if (payee == SmsParser.NO_PAYEE) "$amount UPI payment" else "$amount paid to $payee"
    return TagPromptText(title, TAG_PROMPT_TEXT)
}

/** Room ids start at 1 and fit in an Int; a larger id is folded instead of crashing. */
fun notificationIdFor(transactionId: Long): Int =
    if (transactionId in Int.MIN_VALUE..Int.MAX_VALUE) {
        transactionId.toInt()
    } else {
        (transactionId xor (transactionId ushr 32)).toInt()
    }

/** Data URI that makes each payment's body-tap PendingIntent distinct, e.g. "paisapet://transaction/42". */
fun openUri(transactionId: Long): String = "paisapet://transaction/$transactionId"

/** Request code of the body-tap PendingIntent; activity PendingIntents never clash with the broadcast ones. */
fun openRequestCodeFor(transactionId: Long): Int = notificationIdFor(transactionId)

/** Data URI that makes each action's PendingIntent distinct, e.g. "paisapet://tag/42/MISC". */
fun actionUri(transactionId: Long, category: Category): String {
    requireTagCategory(category)
    return "paisapet://tag/$transactionId/${category.name}"
}

/** Distinct per (transaction, category) for ordinary ids; [actionUri] keeps intents apart regardless. */
fun requestCodeFor(transactionId: Long, category: Category): Int {
    requireTagCategory(category)
    val offset = if (category == Category.MISC) 0 else 1
    return notificationIdFor(transactionId) * 2 + offset
}

private fun requireTagCategory(category: Category) =
    require(category == Category.MISC || category == Category.ESSENTIAL) { "Not a tag action: $category" }
