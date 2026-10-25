package com.example.paisapet.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.paisapet.R
import com.example.paisapet.edit.ActionResult
import com.example.paisapet.edit.AddCheck
import com.example.paisapet.edit.AddError
import com.example.paisapet.edit.AddInput
import com.example.paisapet.edit.acceptAmountText
import com.example.paisapet.edit.acceptPayeeText
import com.example.paisapet.edit.addFieldErrors
import com.example.paisapet.edit.checkAdd
import com.example.paisapet.edit.dateToPickerUtcMillis
import com.example.paisapet.edit.isSelectablePickerMillis
import com.example.paisapet.edit.pickerUtcMillisToDate
import com.example.paisapet.model.Category
import com.example.paisapet.ui.common.CategoryToggle
import com.example.paisapet.ui.onboarding.PrimaryPillButton
import com.example.paisapet.ui.theme.InkSecondary
import com.example.paisapet.ui.theme.SmsIcon
import com.example.paisapet.ui.theme.SmsTile
import com.example.paisapet.ui.theme.SurfaceContainer
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Test tags used by AddExpenseSheetTest. */
object AddExpenseTags {
    const val AMOUNT_FIELD = "add_amount_field"
    const val PAYEE_FIELD = "add_payee_field"
    const val DATE_TIME = "add_date_time"
    const val ADD = "add_button"
}

private enum class AddDialog { NONE, DATE, TIME }

/**
 * S8 as a bottom sheet. [onAdd] is PaymentActions.add: it checks the input again with a fresh clock and saves.
 * [clock] gives "now" and the phone's current zone at every read. Swipe-down and Back call [onClose].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    clock: Clock,
    onAdd: suspend (input: AddInput, now: Instant, zone: ZoneId) -> ActionResult,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Defaults to now (to the minute) when the sheet opens; everything survives rotation and process death.
    val opened = remember { LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var payeeText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.MISC) }
    var date by rememberSaveable { mutableStateOf(opened.toLocalDate()) }
    var time by rememberSaveable { mutableStateOf(opened.toLocalTime()) }
    var amountTouched by rememberSaveable { mutableStateOf(false) }
    var dateTouched by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf(AddDialog.NONE) }
    var resultErrors by remember { mutableStateOf(emptySet<AddError>()) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    val zone = clock.zone
    val now = clock.instant()
    val today = now.atZone(zone).toLocalDate()
    val input = AddInput(amountText, payeeText, category, LocalDateTime.of(date, time))
    val checkErrors = (checkAdd(input, now, zone) as? AddCheck.Invalid)?.errors.orEmpty()
    val fieldErrors = addFieldErrors(checkErrors + resultErrors, amountTouched, dateTouched, submitted)

    fun add() {
        focusManager.clearFocus()
        submitted = true
        resultErrors = emptySet()
        failed = false
        val freshNow = clock.instant()
        val freshZone = clock.zone
        if (checkAdd(input, freshNow, freshZone) is AddCheck.Invalid) return // the errors show under the fields
        saving = true
        scope.launch {
            when (val result = onAdd(input, freshNow, freshZone)) {
                ActionResult.Done -> {
                    try {
                        sheetState.hide()
                    } finally {
                        onClose()
                    }
                }
                is ActionResult.Invalid -> resultErrors = result.errors
                else -> failed = true
            }
            saving = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // Scrolls so the fields stay reachable above the keyboard; ModalBottomSheet adds the IME padding itself.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add expense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = acceptAmountText(amountText, it)
                    amountTouched = true
                    resultErrors = emptySet()
                },
                label = { Text("Amount") },
                prefix = { Text("₹") },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall,
                isError = fieldErrors.amount != null,
                supportingText = fieldErrors.amount?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().testTag(AddExpenseTags.AMOUNT_FIELD),
            )
            OutlinedTextField(
                value = payeeText,
                onValueChange = { payeeText = acceptPayeeText(it) },
                label = { Text("Paid to (optional)") },
                placeholder = { Text("e.g., Chai stall, auto rickshaw") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().testTag(AddExpenseTags.PAYEE_FIELD),
            )

            Text("Classification", style = MaterialTheme.typography.titleSmall)
            CategoryToggle(selected = category, onSelect = { category = it }, enabled = !saving)

            Surface(
                onClick = {
                    focusManager.clearFocus()
                    dialog = AddDialog.DATE
                },
                enabled = !saving,
                shape = CircleShape,
                color = SurfaceContainer,
                modifier = Modifier.testTag(AddExpenseTags.DATE_TIME),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_schedule), contentDescription = null, tint = SmsIcon, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(addDateTimeText(LocalDateTime.of(date, time), today), style = MaterialTheme.typography.titleSmall)
                }
            }
            fieldErrors.date?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Surface(shape = RoundedCornerShape(16.dp), color = SmsTile, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = SmsIcon, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "For payments the app missed, like small UPI payments without SMS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                    )
                }
            }

            if (failed) {
                Text("Couldn't save. Try again.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            PrimaryPillButton(
                text = if (saving) "Saving…" else "Add expense",
                onClick = ::add,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().testTag(AddExpenseTags.ADD),
            )
        }

        when (dialog) {
            AddDialog.NONE -> Unit
            AddDialog.DATE -> {
                // The picker's rule is the pure one: the 1st of this month up to today, in the picker's UTC millis.
                val selectable = remember(today) {
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long) = isSelectablePickerMillis(utcTimeMillis, today)
                        override fun isSelectableYear(year: Int) = year == today.year
                    }
                }
                val pickerState = rememberDatePickerState(
                    initialSelectedDateMillis = dateToPickerUtcMillis(date),
                    yearRange = today.year..today.year,
                    selectableDates = selectable,
                )
                DatePickerDialog(
                    onDismissRequest = { dialog = AddDialog.NONE },
                    confirmButton = {
                        TextButton(onClick = {
                            pickerState.selectedDateMillis?.let { date = pickerUtcMillisToDate(it) }
                            dateTouched = true
                            resultErrors = emptySet()
                            dialog = AddDialog.TIME
                        }) { Text("Next") }
                    },
                    dismissButton = { TextButton(onClick = { dialog = AddDialog.NONE }) { Text("Cancel") } },
                ) {
                    DatePicker(state = pickerState)
                }
            }
            AddDialog.TIME -> {
                val timeState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = false)
                AlertDialog(
                    onDismissRequest = { dialog = AddDialog.NONE },
                    confirmButton = {
                        TextButton(onClick = {
                            time = LocalTime.of(timeState.hour, timeState.minute)
                            dateTouched = true
                            resultErrors = emptySet()
                            dialog = AddDialog.NONE
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { dialog = AddDialog.NONE }) { Text("Cancel") } },
                    text = { TimePicker(state = timeState) },
                )
            }
        }
    }
}
