package com.example.paisapet.onboarding

/**
 * Commas for the S2 field while typing: "100000" → "1,00,000" (last three digits, then groups of two),
 * plus the cursor mapping between the plain digits and the grouped text. The Compose wrapper lives with S2.
 */
object IndianGrouping {

    fun group(digits: String): String {
        val out = StringBuilder()
        digits.forEachIndexed { i, c ->
            if (hasCommaBefore(i, digits.length)) out.append(',')
            out.append(c)
        }
        return out.toString()
    }

    /** Cursor [offset] in [digits] → offset in [group]. A cursor next to a comma stays left of it. */
    fun originalToTransformed(digits: String, offset: Int): Int {
        val o = offset.coerceIn(0, digits.length)
        return o + (1 until o).count { hasCommaBefore(it, digits.length) }
    }

    /** Cursor [offset] in [group] → offset in [digits]: the digits to its left. */
    fun transformedToOriginal(digits: String, offset: Int): Int {
        val grouped = group(digits)
        val t = offset.coerceIn(0, grouped.length)
        return (0 until t).count { grouped[it] != ',' }
    }

    /** True when a comma goes just before digit [index] of a [length]-digit number. */
    private fun hasCommaBefore(index: Int, length: Int): Boolean {
        val right = length - index
        return index > 0 && right >= 3 && (right - 3) % 2 == 0
    }
}
