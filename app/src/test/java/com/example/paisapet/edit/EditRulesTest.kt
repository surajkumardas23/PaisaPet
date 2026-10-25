package com.example.paisapet.edit

import com.example.paisapet.model.Category
import com.example.paisapet.model.Source
import com.example.paisapet.tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditRulesTest {

    private val smsUntagged = tx(category = Category.UNTAGGED, source = Source.SMS)
    private val smsMisc = tx(category = Category.MISC, source = Source.SMS)
    private val manual = tx(amountPaise = 2_000, payee = "Chai", category = Category.MISC, source = Source.MANUAL)

    @Test fun untaggedStartsWithNothingSelectedAndSaveDisabled() {
        val f = initialEditForm(smsUntagged)
        assertNull(f.selected)
        assertEquals(EditChange.None, f.change())
        assertFalse(f.canSave(readOnly = false))
    }

    @Test fun pickingACategoryOnUntaggedEnablesSave() {
        val f = initialEditForm(smsUntagged).copy(selected = Category.MISC)
        assertEquals(EditChange.Tag(Category.MISC), f.change())
        assertTrue(f.canSave(readOnly = false))
    }

    @Test fun miscStartsAsMiscAndSameChoiceIsNoChange() {
        val f = initialEditForm(smsMisc)
        assertEquals(Category.MISC, f.selected)
        assertEquals(EditChange.None, f.change())
        assertFalse(f.canSave(readOnly = false))
    }

    @Test fun smsRowCanOnlyProduceTag() {
        val f = initialEditForm(smsMisc).copy(selected = Category.ESSENTIAL, amountText = "999", payeeText = "Other")
        assertEquals(EditChange.Tag(Category.ESSENTIAL), f.change())
    }

    @Test fun smsRowIgnoresAmountAndPayeeEdits() {
        val f = initialEditForm(smsMisc).copy(amountText = "999", payeeText = "Other")
        assertEquals(EditChange.None, f.change())
    }

    @Test fun manualStartsWithItsValues() {
        val f = initialEditForm(manual.copy(amountPaise = 2_050))
        assertEquals("20.50", f.amountText)
        assertEquals("Chai", f.payeeText)
        assertEquals(Category.MISC, f.selected)
    }

    @Test fun manualAmountChangeGivesManual() {
        val f = initialEditForm(manual).copy(amountText = "25")
        assertEquals(EditChange.Manual(2_500, "Chai", Category.MISC), f.change())
    }

    @Test fun manualPayeeChangeIsNormalised() {
        val f = initialEditForm(manual).copy(payeeText = "  ")
        assertEquals(EditChange.Manual(2_000, MANUAL_PAYEE, Category.MISC), f.change())
    }

    @Test fun manualCategoryOnlyGivesTag() {
        val f = initialEditForm(manual).copy(selected = Category.ESSENTIAL)
        assertEquals(EditChange.Tag(Category.ESSENTIAL), f.change())
    }

    @Test fun manualAmountAndCategoryTogether() {
        val f = initialEditForm(manual).copy(amountText = "30", selected = Category.ESSENTIAL)
        assertEquals(EditChange.Manual(3_000, "Chai", Category.ESSENTIAL), f.change())
    }

    @Test fun manualSameValuesAfterTypingIsNoChange() {
        val f = initialEditForm(manual).copy(amountText = "20.00", payeeText = " Chai ")
        assertEquals(EditChange.None, f.change())
    }

    @Test fun invalidAmountGivesNone() {
        for (text in listOf("", "0", "abc", "1000000.01")) {
            val f = initialEditForm(manual).copy(amountText = text, selected = Category.ESSENTIAL)
            assertEquals(text, EditChange.None, f.change())
            assertFalse(f.canSave(readOnly = false))
        }
    }

    @Test fun readOnlyNeverSaves() {
        val f = initialEditForm(smsUntagged).copy(selected = Category.ESSENTIAL)
        assertFalse(f.canSave(readOnly = true))
    }

    @Test fun amountAndPayeeEditableOnlyForManualAndNotReadOnly() {
        assertTrue(canEditAmountAndPayee(manual, readOnly = false))
        assertFalse(canEditAmountAndPayee(manual, readOnly = true))
        assertFalse(canEditAmountAndPayee(smsMisc, readOnly = false))
    }
}
