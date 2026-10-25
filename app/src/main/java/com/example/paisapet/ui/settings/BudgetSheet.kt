package com.example.paisapet.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.paisapet.onboarding.BudgetCheck
import com.example.paisapet.onboarding.BudgetInput
import com.example.paisapet.ui.onboarding.BudgetAmountCard
import com.example.paisapet.ui.onboarding.BudgetChips
import com.example.paisapet.ui.onboarding.PrimaryPillButton
import com.example.paisapet.ui.theme.InkSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Edits the monthly misc budget (S9 pencil) with the S2 input. [onSave] is container.saveBudget: it saves the
 * default for future months and this month's budget, keeping the alert flags. Back and swipe-down call [onClose].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSheet(
    currentBudgetPaise: Long?,
    onSave: suspend (budgetPaise: Long) -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf(BudgetInput.prefillText(currentBudgetPaise)) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val check = BudgetInput.check(text)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        // Scrolls so the field stays reachable above the keyboard; ModalBottomSheet adds the IME padding itself.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Monthly misc budget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            BudgetAmountCard(text, check) {
                text = it
                failed = false
            }
            Text(
                "Applies to this month and every month after it.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
            )
            BudgetChips(selected = BudgetInput.selectedChip(text), onPick = {
                text = it.toString()
                failed = false
            })
            if (failed) {
                Text("Couldn't save. Try again.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            PrimaryPillButton(
                text = if (saving) "Saving…" else "Save budget",
                enabled = check is BudgetCheck.Valid && !saving,
                onClick = {
                    val paise = (check as? BudgetCheck.Valid)?.paise ?: return@PrimaryPillButton
                    saving = true
                    failed = false
                    scope.launch {
                        val saved = try {
                            onSave(paise)
                            true
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            false
                        }
                        saving = false
                        if (saved) {
                            try {
                                sheetState.hide()
                            } finally {
                                onClose()
                            }
                        } else {
                            failed = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
