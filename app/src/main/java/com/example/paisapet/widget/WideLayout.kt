package com.example.paisapet.widget

// Pure: which parts of the wide widget fit at its current size. Sizes in dp, matching WideWidget's layout.

/** Buddy and the % chip sit in a column this wide, left of the numbers. */
const val WIDE_LEFT_COLUMN_DP = 76f
const val WIDE_COLUMN_GAP_DP = 12f

/** Narrower than this, Buddy's column is dropped and the % chip moves into the numbers column. */
const val WIDE_BUDDY_MIN_WIDTH_DP = 230f

/** Shorter than these, the last payment line and then the budget line are dropped. */
const val WIDE_LAST_PAYMENT_MIN_HEIGHT_DP = 108f
const val WIDE_BUDGET_LINE_MIN_HEIGHT_DP = 124f

data class WideFit(
    val showBuddy: Boolean,
    val showBudgetLine: Boolean,
    val showLastPayment: Boolean,
    /** Width of the bar's track in the numbers column; never negative. */
    val barTrackDp: Float,
)

/** [widthDp] and [heightDp] are the whole widget; [paddingDp] is the card's padding on each side. */
fun wideFit(widthDp: Float, heightDp: Float, paddingDp: Float): WideFit {
    val showBuddy = widthDp >= WIDE_BUDDY_MIN_WIDTH_DP
    val inner = widthDp - paddingDp * 2 - if (showBuddy) WIDE_LEFT_COLUMN_DP + WIDE_COLUMN_GAP_DP else 0f
    return WideFit(
        showBuddy = showBuddy,
        showBudgetLine = heightDp >= WIDE_BUDGET_LINE_MIN_HEIGHT_DP,
        showLastPayment = heightDp >= WIDE_LAST_PAYMENT_MIN_HEIGHT_DP,
        barTrackDp = inner.coerceAtLeast(0f),
    )
}
