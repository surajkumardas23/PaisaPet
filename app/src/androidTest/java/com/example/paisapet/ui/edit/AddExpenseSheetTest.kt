package com.example.paisapet.ui.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.paisapet.edit.ActionResult
import com.example.paisapet.edit.AddInput
import com.example.paisapet.model.Category
import com.example.paisapet.ui.theme.PaisaPetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

/** S8 with a fixed clock and a fake save. Runs on the phone with connectedAndroidTest. */
@RunWith(AndroidJUnit4::class)
class AddExpenseSheetTest {

    @get:Rule val compose = createComposeRule()

    private val ist: ZoneId = ZoneId.of("Asia/Kolkata")
    private val clock: Clock = Clock.fixed(LocalDateTime.of(2026, 10, 25, 15, 30).atZone(ist).toInstant(), ist)

    private val added = mutableListOf<AddInput>()
    private var closed = false

    private fun show(result: ActionResult = ActionResult.Done) {
        compose.setContent {
            PaisaPetTheme {
                AddExpenseSheet(
                    clock = clock,
                    onAdd = { input, _, _ ->
                        added += input
                        result
                    },
                    onClose = { closed = true },
                )
            }
        }
    }

    @Test fun emptyAmount_showsErrorAndSavesNothing() {
        show()
        compose.onNodeWithTag(AddExpenseTags.ADD).performClick()
        compose.onNodeWithText("Enter an amount").assertIsDisplayed()
        assertTrue(added.isEmpty())
        assertEquals(false, closed)
    }

    @Test fun zeroAmount_isRejected() {
        show()
        compose.onNodeWithTag(AddExpenseTags.AMOUNT_FIELD).performTextInput("0")
        compose.onNodeWithText("Amount must be more than ₹0").assertIsDisplayed()
        compose.onNodeWithTag(AddExpenseTags.ADD).performClick()
        assertTrue(added.isEmpty())
    }

    @Test fun validAmount_savesAsMiscNowAndCloses() {
        show()
        compose.onNodeWithText("Today, 3:30 PM").assertIsDisplayed()
        compose.onNodeWithTag(AddExpenseTags.AMOUNT_FIELD).performTextInput("20")
        compose.onNodeWithTag(AddExpenseTags.ADD).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { closed }
        val input = added.single()
        assertEquals("20", input.amountText)
        assertEquals("", input.payeeText)
        assertEquals(Category.MISC, input.category)
        assertEquals(LocalDateTime.of(2026, 10, 25, 15, 30), input.occurredAt)
    }

    @Test fun failedSave_staysOpenWithMessage() {
        show(result = ActionResult.Failed)
        compose.onNodeWithTag(AddExpenseTags.AMOUNT_FIELD).performTextInput("20")
        compose.onNodeWithTag(AddExpenseTags.ADD).performClick()
        compose.onNodeWithText("Couldn't save. Try again.").assertIsDisplayed()
        assertEquals(false, closed)
    }
}
