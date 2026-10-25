package com.example.paisapet.ui.edit

import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.edit.ActionResult
import com.example.paisapet.edit.canEditAmountAndPayee
import com.example.paisapet.format.formatRupees
import com.example.paisapet.list.isReadOnly
import com.example.paisapet.model.Source
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Pure: what the Edit sheet (S7) shows and what it does after a write. No Compose here.

private val DATE_TIME = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)

const val PAST_MONTH_TEXT = "From a past month · view only"

data class EditDetails(
    /** "₹240". */
    val amountText: String,
    val payee: String,
    /** "Sun, 25 Oct 2026 · 1:15 PM". */
    val dateTimeText: String,
    /** "From SMS" or "Added manually". */
    val sourceText: String,
    val manual: Boolean,
    val readOnly: Boolean,
    /** Manual rows in the current month only. */
    val canEditAmountAndPayee: Boolean,
)

fun editDetails(t: TransactionEntity, currentKey: String, zone: ZoneId): EditDetails {
    val readOnly = isReadOnly(t.monthKey, currentKey)
    return EditDetails(
        amountText = formatRupees(t.amountPaise),
        payee = t.payee,
        dateTimeText = Instant.ofEpochMilli(t.occurredAt).atZone(zone).format(DATE_TIME),
        sourceText = if (t.source == Source.MANUAL) "Added manually" else "From SMS",
        manual = t.source == Source.MANUAL,
        readOnly = readOnly,
        canEditAmountAndPayee = canEditAmountAndPayee(t, readOnly),
    )
}

/** What the sheet does after Save or Delete returns. */
sealed interface EditOutcome {
    /** Done: close the sheet. */
    data object Close : EditOutcome

    /** The payment is gone: close and say so. */
    data object Deleted : EditOutcome

    /** Stay open and show [message]. */
    data class Error(val message: String) : EditOutcome
}

fun editOutcome(result: ActionResult, deleting: Boolean): EditOutcome = when (result) {
    ActionResult.Done -> EditOutcome.Close
    ActionResult.NotFound -> EditOutcome.Deleted
    ActionResult.ReadOnly -> EditOutcome.Error("This payment is now in a past month and can't be changed.")
    is ActionResult.Invalid -> EditOutcome.Error("Check the amount and try again.")
    ActionResult.Failed -> EditOutcome.Error(if (deleting) "Couldn't delete. Try again." else "Couldn't save. Try again.")
}
