package com.example.paisapet.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paisapet.AppContainer
import com.example.paisapet.R
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.edit.AmountCheck
import com.example.paisapet.edit.EditForm
import com.example.paisapet.edit.acceptAmountText
import com.example.paisapet.edit.acceptPayeeText
import com.example.paisapet.edit.amountErrorMessage
import com.example.paisapet.edit.canSave
import com.example.paisapet.edit.change
import com.example.paisapet.edit.checkAmount
import com.example.paisapet.edit.initialEditForm
import com.example.paisapet.model.Category
import com.example.paisapet.ui.common.CategoryToggle
import com.example.paisapet.ui.theme.HelperPill
import com.example.paisapet.ui.theme.InkSecondary
import com.example.paisapet.ui.theme.OverBudgetRed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Test tags used by EditSheetTest. */
object EditSheetTags {
    const val AMOUNT_FIELD = "edit_amount_field"
    const val PAYEE_FIELD = "edit_payee_field"
    const val SAVE = "edit_save"
    const val DELETE = "edit_delete"
}

/** Wraps the row so "still loading" (null state) differs from "deleted" (null row). */
private data class RowState(val row: TransactionEntity?)

private enum class Busy { SAVING, DELETING }

/**
 * S7 as a bottom sheet for payment [id]. Swipe-down and Back call [onClose]. If the payment disappears
 * while open (deleted elsewhere, or an id that never existed), [onDeleted] closes it with a message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheet(
    id: Long,
    container: AppContainer,
    currentKey: String,
    onClose: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by remember(id) { container.repository.observeTransaction(id).map { RowState(it) } }
        .collectAsStateWithLifecycle(initialValue = null)
    // Set when this sheet deleted the row itself, so its own delete is not reported as "deleted meanwhile".
    var deletedHere by remember(id) { mutableStateOf(false) }

    val loaded = state ?: return
    val row = loaded.row
    if (row == null) {
        LaunchedEffect(id) { if (deletedHere) onClose() else onDeleted() }
        return
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // The form survives re-emissions of the same row and process death; it starts from the row once.
    val initial = remember(row.id) { initialEditForm(row) }
    var selected by rememberSaveable(row.id) { mutableStateOf(initial.selected) }
    var amountText by rememberSaveable(row.id) { mutableStateOf(initial.amountText) }
    var payeeText by rememberSaveable(row.id) { mutableStateOf(initial.payeeText) }
    var busy by remember { mutableStateOf<Busy?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val form = EditForm(row, selected, amountText, payeeText)
    val details = editDetails(row, currentKey, container.zone)

    fun finish(outcome: EditOutcome) {
        when (outcome) {
            EditOutcome.Close -> scope.launch {
                try {
                    sheetState.hide()
                } finally {
                    onClose()
                }
            }
            EditOutcome.Deleted -> onDeleted()
            is EditOutcome.Error -> error = outcome.message
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        EditSheetBody(
            details = details,
            form = form,
            saving = busy == Busy.SAVING,
            busy = busy != null,
            error = error,
            onSelect = { selected = it },
            onAmountChange = { amountText = it },
            onPayeeChange = { payeeText = it },
            onSave = {
                val change = form.change()
                busy = Busy.SAVING
                error = null
                scope.launch {
                    val result = container.paymentActions.saveEdit(row, change)
                    busy = null
                    finish(editOutcome(result, deleting = false))
                }
            },
            onConfirmDelete = {
                busy = Busy.DELETING
                error = null
                scope.launch {
                    val outcome = editOutcome(container.paymentActions.delete(row), deleting = true)
                    busy = null
                    if (outcome == EditOutcome.Close) {
                        deletedHere = true
                        onClose()
                    } else {
                        finish(outcome)
                    }
                }
            },
        )
    }
}

/** The sheet's contents, without the sheet, so it can be tested on its own. */
@Composable
fun EditSheetBody(
    details: EditDetails,
    form: EditForm,
    saving: Boolean,
    busy: Boolean,
    error: String?,
    onSelect: (Category) -> Unit,
    onAmountChange: (String) -> Unit,
    onPayeeChange: (String) -> Unit,
    onSave: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    // Scrolls so the fields stay reachable above the keyboard; ModalBottomSheet adds the IME padding itself.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Transaction details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (details.readOnly) {
            Surface(shape = CircleShape, color = HelperPill) {
                Text(
                    PAST_MONTH_TEXT,
                    style = MaterialTheme.typography.labelLarge,
                    color = InkSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        if (details.canEditAmountAndPayee) {
            val amountError = (checkAmount(form.amountText) as? AmountCheck.Error)?.let { amountErrorMessage(it.error) }
            OutlinedTextField(
                value = form.amountText,
                onValueChange = { onAmountChange(acceptAmountText(form.amountText, it)) },
                label = { Text("Amount") },
                prefix = { Text("₹") },
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag(EditSheetTags.AMOUNT_FIELD),
            )
            OutlinedTextField(
                value = form.payeeText,
                onValueChange = { onPayeeChange(acceptPayeeText(it)) },
                label = { Text("Paid to") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag(EditSheetTags.PAYEE_FIELD),
            )
        } else {
            Text(details.amountText, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(details.payee, style = MaterialTheme.typography.titleMedium)
        }

        InfoLine(R.drawable.ic_schedule, details.dateTimeText)
        InfoLine(if (details.manual) R.drawable.ic_edit else R.drawable.ic_receipt, details.sourceText)

        if (!details.readOnly) {
            Spacer(Modifier.height(4.dp))
            Text("How should this be counted?", style = MaterialTheme.typography.titleSmall)
            CategoryToggle(selected = form.selected, onSelect = onSelect, enabled = !busy)
            Text(
                "Misc reduces your monthly budget. Essential stays outside it.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
            )
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onSave,
                enabled = form.canSave(details.readOnly) && !busy,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag(EditSheetTags.SAVE),
            ) {
                Icon(painterResource(R.drawable.ic_check_circle), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (saving) "Saving…" else "Save changes", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(
                onClick = { confirmDelete = true },
                enabled = !busy,
                modifier = Modifier.align(Alignment.CenterHorizontally).testTag(EditSheetTags.DELETE),
            ) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = OverBudgetRed, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete transaction", color = OverBudgetRed, style = MaterialTheme.typography.titleSmall)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this payment?") },
            text = { Text("It will be removed from this month's totals. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onConfirmDelete()
                }) { Text("Delete", color = OverBudgetRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun InfoLine(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = InkSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
    }
}
