package com.example.paisapet.edit

import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source

/** What the Edit sheet (S7) holds. [selected] is null while a Not tagged payment has no choice yet. */
data class EditForm(
    val original: TransactionEntity,
    val selected: Category?,
    val amountText: String,
    val payeeText: String,
)

/** What Save would write. */
sealed interface EditChange {
    data object None : EditChange
    data class Tag(val category: Category) : EditChange
    data class Manual(val amountPaise: Long, val payee: String, val category: Category) : EditChange
}

/** A Not tagged payment starts with nothing selected, so opening the sheet never tags it by accident. */
fun initialEditForm(t: TransactionEntity): EditForm = EditForm(
    original = t,
    selected = t.category.takeUnless { it == Category.UNTAGGED },
    amountText = amountTextOf(t.amountPaise),
    payeeText = t.payee,
)

/** SMS payments keep amount and payee read-only (FR-13); past months are read-only entirely. */
fun canEditAmountAndPayee(t: TransactionEntity, readOnly: Boolean): Boolean = t.source == Source.MANUAL && !readOnly

/** [EditChange.None] when nothing changed or the amount is invalid. */
fun EditForm.change(): EditChange {
    val category = selected ?: original.category
    val categoryChanged = selected != null && selected != original.category
    if (original.source != Source.MANUAL) {
        return if (categoryChanged) EditChange.Tag(category) else EditChange.None
    }
    val amount = (checkAmount(amountText) as? AmountCheck.Valid)?.paise ?: return EditChange.None
    val payee = normalisePayee(payeeText)
    return when {
        amount != original.amountPaise || payee != original.payee -> EditChange.Manual(amount, payee, category)
        categoryChanged -> EditChange.Tag(category)
        else -> EditChange.None
    }
}

fun EditForm.canSave(readOnly: Boolean): Boolean = !readOnly && change() != EditChange.None
