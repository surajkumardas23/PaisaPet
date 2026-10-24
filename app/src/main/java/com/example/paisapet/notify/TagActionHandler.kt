package com.example.paisapet.notify

import com.example.paisapet.model.Category
import com.example.paisapet.model.TagSource
import kotlinx.coroutines.CancellationException

sealed interface TagOutcome {
    data object Tagged : TagOutcome
    /** The transaction was deleted; the prompt is dismissed (PRD edge case). */
    data object NotFound : TagOutcome
    /** Bad id or category in the intent; nothing written. */
    data object Invalid : TagOutcome
    /** The write threw; the prompt stays so the user can retry. */
    data object Failed : TagOutcome
}

/** What a Misc or Essential tap on the tag prompt does (FR-07). */
class TagActionHandler(
    private val setCategory: suspend (id: Long, category: Category, tagSource: TagSource) -> Boolean,
    private val cancelPrompt: (id: Long) -> Unit,
    /** The widget refresh goes here later. */
    private val onDataChanged: () -> Unit = {},
) {

    /** Never throws, except [CancellationException]. */
    suspend fun handle(transactionId: Long, categoryName: String?): TagOutcome {
        val category = when (categoryName) {
            Category.MISC.name -> Category.MISC
            Category.ESSENTIAL.name -> Category.ESSENTIAL
            else -> null
        }
        if (transactionId < 1 || category == null) return TagOutcome.Invalid

        val found = try {
            setCategory(transactionId, category, TagSource.NOTIFICATION)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return TagOutcome.Failed
        }
        try {
            cancelPrompt(transactionId)
        } catch (e: Exception) {
            // The tag is saved; a prompt left on screen is harmless.
        }
        if (!found) return TagOutcome.NotFound
        try {
            onDataChanged()
        } catch (e: Exception) {
            // The tag is saved and the UI observes Room.
        }
        return TagOutcome.Tagged
    }
}
