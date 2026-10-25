package com.example.paisapet.edit

import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.model.TagSource
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneId

/** The repository writes the screens need; the repository implements it, tests use a fake. */
interface PaymentStore {
    /** False if [id] no longer exists. */
    suspend fun setCategory(id: Long, category: Category, tagSource: TagSource): Boolean

    /** Manual rows only; false if [id] is missing or not manual. */
    suspend fun updateManual(id: Long, amountPaise: Long, payee: String, category: Category): Boolean

    /** False if [id] no longer exists. */
    suspend fun delete(id: Long): Boolean

    /** Returns the new row's id. */
    suspend fun addManual(expense: ManualExpense): Long
}

sealed interface ActionResult {
    data object Done : ActionResult
    data object NotFound : ActionResult
    /** The payment is in a past month; nothing was written. */
    data object ReadOnly : ActionResult
    data class Invalid(val errors: Set<AddError> = emptySet()) : ActionResult
    /** The write threw; the sheet stays open so the user can retry. */
    data object Failed : ActionResult
}

/** Save, delete and add from the app. A tag, edit or delete also removes that payment's pending prompt. */
class PaymentActions(
    private val store: PaymentStore,
    private val cancelPrompt: (id: Long) -> Unit,
    private val currentMonthKey: () -> String,
    /** Called after every successful tag, edit, delete or add (widgets, alerts). */
    private val onDataChanged: suspend () -> Unit = {},
) {

    suspend fun saveEdit(original: TransactionEntity, change: EditChange): ActionResult {
        if (original.monthKey != currentMonthKey()) return ActionResult.ReadOnly
        invalidReason(original, change)?.let { return it }
        return writeThenCancel(original.id) {
            when (change) {
                is EditChange.Tag -> store.setCategory(original.id, change.category, TagSource.APP)
                is EditChange.Manual -> store.updateManual(original.id, change.amountPaise, change.payee, change.category)
                EditChange.None -> false // Unreachable: invalidReason rejects it.
            }
        }
    }

    private fun invalidReason(original: TransactionEntity, change: EditChange): ActionResult.Invalid? = when (change) {
        EditChange.None -> ActionResult.Invalid()
        is EditChange.Tag -> if (change.category == Category.UNTAGGED) ActionResult.Invalid() else null
        is EditChange.Manual -> when {
            original.source != Source.MANUAL || change.category == Category.UNTAGGED -> ActionResult.Invalid()
            change.amountPaise <= 0 -> ActionResult.Invalid(setOf(AddError.AMOUNT_ZERO))
            change.amountPaise > MAX_AMOUNT_PAISE -> ActionResult.Invalid(setOf(AddError.AMOUNT_TOO_LARGE))
            else -> null
        }
    }

    suspend fun delete(original: TransactionEntity): ActionResult {
        if (original.monthKey != currentMonthKey()) return ActionResult.ReadOnly
        return writeThenCancel(original.id) { store.delete(original.id) }
    }

    /** Checks [input] again with [now], in case the month changed while the sheet was open. Posts no notification. */
    suspend fun add(input: AddInput, now: Instant, zone: ZoneId): ActionResult {
        val expense = when (val check = checkAdd(input, now, zone)) {
            is AddCheck.Invalid -> return ActionResult.Invalid(check.errors)
            is AddCheck.Valid -> check.expense
        }
        try {
            store.addManual(expense)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ActionResult.Failed
        }
        dataChanged()
        return ActionResult.Done
    }

    private suspend fun writeThenCancel(id: Long, write: suspend () -> Boolean): ActionResult {
        val found = try {
            write()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ActionResult.Failed
        }
        if (!found) return ActionResult.NotFound
        try {
            cancelPrompt(id)
        } catch (e: Exception) {
            // The write is saved; a prompt left on screen is harmless.
        }
        dataChanged()
        return ActionResult.Done
    }

    /** A failing hook never changes a Done result. */
    private suspend fun dataChanged() {
        try {
            onDataChanged()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The write is saved; screens observe Room.
        }
    }
}
