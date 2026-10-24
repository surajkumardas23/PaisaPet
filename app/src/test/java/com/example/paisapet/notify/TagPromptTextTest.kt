package com.example.paisapet.notify

import com.example.paisapet.model.Category.ESSENTIAL
import com.example.paisapet.model.Category.MISC
import com.example.paisapet.model.Category.UNTAGGED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TagPromptTextTest {

    @Test fun titleAndText() =
        assertEquals(
            TagPromptText("₹240 paid to Swiggy", "Misc or Essential spend? Tap to tag."),
            tagPromptText(24000, "Swiggy"),
        )

    @Test fun paiseShownInTitle() = assertEquals("₹776.63 paid to Zepto", tagPromptText(77663, "Zepto").title)

    @Test fun fallbackPayee() = assertEquals("₹240 UPI payment", tagPromptText(24000, "UPI payment").title)

    @Test fun fortyCharPayeeKeptWhole() {
        val payee = "A".repeat(40)
        assertEquals("₹1,00,000 paid to $payee", tagPromptText(10000000, payee).title)
    }

    @Test fun publicText() = assertEquals("New UPI payment · Unlock to tag", TAG_PROMPT_PUBLIC_TEXT)

    @Test fun notificationIdIsTheTransactionId() {
        assertEquals(1, notificationIdFor(1))
        assertEquals(Int.MAX_VALUE, notificationIdFor(Int.MAX_VALUE.toLong()))
        assertEquals(notificationIdFor(7), notificationIdFor(7))
    }

    @Test fun hugeIdDoesNotCrash() {
        notificationIdFor(Long.MAX_VALUE)
        requestCodeFor(Long.MAX_VALUE, ESSENTIAL)
        assertNotEquals(notificationIdFor(Int.MAX_VALUE + 1L), notificationIdFor(Int.MAX_VALUE + 2L))
    }

    @Test fun requestCodesDistinct() {
        val codes = listOf(requestCodeFor(1, MISC), requestCodeFor(1, ESSENTIAL), requestCodeFor(2, MISC), requestCodeFor(2, ESSENTIAL))
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test fun actionUris() {
        assertEquals("paisapet://tag/42/MISC", actionUri(42, MISC))
        assertEquals("paisapet://tag/42/ESSENTIAL", actionUri(42, ESSENTIAL))
        val uris = listOf(actionUri(1, MISC), actionUri(1, ESSENTIAL), actionUri(2, MISC))
        assertEquals(uris.size, uris.toSet().size)
    }

    @Test fun untaggedIsNotAnAction() {
        assertThrows(IllegalArgumentException::class.java) { actionUri(1, UNTAGGED) }
        assertThrows(IllegalArgumentException::class.java) { requestCodeFor(1, UNTAGGED) }
    }
}
