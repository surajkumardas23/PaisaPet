package com.example.paisapet.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.paisapet.data.db.TransactionEntity
import com.example.paisapet.edit.EditForm
import com.example.paisapet.edit.initialEditForm
import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.ui.theme.PaisaPetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneId

/** S7 body without the sheet or the database. Runs on the phone with connectedAndroidTest. */
@RunWith(AndroidJUnit4::class)
class EditSheetTest {

    @get:Rule val compose = createComposeRule()

    private val ist: ZoneId = ZoneId.of("Asia/Kolkata")
    private val occurredAt = LocalDateTime.of(2026, 10, 25, 13, 15).atZone(ist).toInstant().toEpochMilli()

    private fun row(category: Category, source: Source) = TransactionEntity(
        id = 7,
        amountPaise = 240_00,
        payee = "Swiggy",
        upiRef = null,
        bank = "HDFCBK",
        occurredAt = occurredAt,
        monthKey = "2026-10",
        category = category,
        source = source,
        tagSource = null,
        taggedAt = null,
        smsHash = if (source == Source.SMS) "h7" else null,
        createdAt = occurredAt,
        updatedAt = occurredAt,
    )

    /** Shows the body with real form state, as EditSheet does, but with no writes. */
    private fun show(t: TransactionEntity, onConfirmDelete: () -> Unit = {}) {
        compose.setContent {
            PaisaPetTheme {
                var form by remember { mutableStateOf<EditForm>(initialEditForm(t)) }
                EditSheetBody(
                    details = editDetails(t, "2026-10", ist),
                    form = form,
                    saving = false,
                    busy = false,
                    error = null,
                    onSelect = { form = form.copy(selected = it) },
                    onAmountChange = { form = form.copy(amountText = it) },
                    onPayeeChange = { form = form.copy(payeeText = it) },
                    onSave = {},
                    onConfirmDelete = onConfirmDelete,
                )
            }
        }
    }

    @Test fun smsRow_amountAndPayeeAreNotEditable() {
        show(row(Category.MISC, Source.SMS))
        compose.onNodeWithText("₹240").assertIsDisplayed()
        compose.onNodeWithText("From SMS").assertIsDisplayed()
        compose.onNodeWithTag(EditSheetTags.AMOUNT_FIELD).assertDoesNotExist()
        compose.onNodeWithTag(EditSheetTags.PAYEE_FIELD).assertDoesNotExist()
    }

    @Test fun manualRow_amountAndPayeeAreEditable() {
        show(row(Category.MISC, Source.MANUAL))
        compose.onNodeWithTag(EditSheetTags.AMOUNT_FIELD).assertIsDisplayed()
        compose.onNodeWithTag(EditSheetTags.PAYEE_FIELD).assertIsDisplayed()
    }

    @Test fun notTagged_saveDisabledUntilMiscChosen() {
        show(row(Category.UNTAGGED, Source.SMS))
        compose.onNodeWithTag(EditSheetTags.SAVE).assertIsNotEnabled()
        compose.onNodeWithText("Misc").performClick()
        compose.onNodeWithTag(EditSheetTags.SAVE).assertIsEnabled()
    }

    @Test fun delete_showsConfirmDialog_andCancelKeepsTheRow() {
        var deleted = false
        show(row(Category.MISC, Source.SMS), onConfirmDelete = { deleted = true })
        compose.onNodeWithTag(EditSheetTags.DELETE).performClick()
        compose.onNodeWithText("Delete this payment?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Delete this payment?").assertDoesNotExist()
        check(!deleted) { "Cancel must not delete" }
    }

    @Test fun delete_confirmCallsDelete() {
        var deleted = false
        show(row(Category.MISC, Source.SMS), onConfirmDelete = { deleted = true })
        compose.onNodeWithTag(EditSheetTags.DELETE).performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        check(deleted) { "Delete must call onConfirmDelete" }
    }

    @Test fun pastMonth_showsViewOnlyAndNoButtons() {
        compose.setContent {
            PaisaPetTheme {
                val t = row(Category.UNTAGGED, Source.SMS)
                EditSheetBody(
                    details = editDetails(t, "2026-11", ist),
                    form = initialEditForm(t),
                    saving = false,
                    busy = false,
                    error = null,
                    onSelect = {},
                    onAmountChange = {},
                    onPayeeChange = {},
                    onSave = {},
                    onConfirmDelete = {},
                )
            }
        }
        compose.onNodeWithText(PAST_MONTH_TEXT).assertIsDisplayed()
        compose.onNodeWithTag(EditSheetTags.SAVE).assertDoesNotExist()
        compose.onNodeWithTag(EditSheetTags.DELETE).assertDoesNotExist()
        compose.onNodeWithText("Misc").assertDoesNotExist()
    }
}
