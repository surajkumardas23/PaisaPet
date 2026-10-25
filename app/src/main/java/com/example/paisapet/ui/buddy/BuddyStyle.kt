package com.example.paisapet.ui.buddy

import androidx.compose.ui.graphics.Color
import com.example.paisapet.budget.BuddyState
import com.example.paisapet.ui.theme.BuddyFainted
import com.example.paisapet.ui.theme.BuddyFaintedBody
import com.example.paisapet.ui.theme.BuddyFaintedTint
import com.example.paisapet.ui.theme.BuddyFine
import com.example.paisapet.ui.theme.BuddyFineBody
import com.example.paisapet.ui.theme.BuddyFineTint
import com.example.paisapet.ui.theme.BuddyGreen
import com.example.paisapet.ui.theme.BuddySick
import com.example.paisapet.ui.theme.BuddySickBody
import com.example.paisapet.ui.theme.BuddySickTint
import com.example.paisapet.ui.theme.BuddyThriving
import com.example.paisapet.ui.theme.BuddyThrivingTint
import com.example.paisapet.ui.theme.BuddyWorried
import com.example.paisapet.ui.theme.BuddyWorriedBody
import com.example.paisapet.ui.theme.BuddyWorriedTint

// The one place for Buddy's per-state text and colours. Phase 7 replaces the art.

/** The state name on the Home chip: "64% used · Worried". */
fun buddyName(state: BuddyState): String = when (state) {
    BuddyState.THRIVING -> "Thriving"
    BuddyState.FINE -> "Fine"
    BuddyState.WORRIED -> "Worried"
    BuddyState.SICK -> "Sick"
    BuddyState.FAINTED -> "Fainted"
}

/** The Home label from the PRD's Buddy states table. */
fun buddyHomeLabel(state: BuddyState): String = when (state) {
    BuddyState.THRIVING -> "Buddy is thriving"
    BuddyState.FINE -> "Pacing smoothly"
    BuddyState.WORRIED -> "Buddy is getting nervous"
    BuddyState.SICK -> "Buddy is getting sick"
    BuddyState.FAINTED -> "Buddy fainted"
}

/** Progress bar, chip dot and chip text. */
fun buddyColor(state: BuddyState): Color = when (state) {
    BuddyState.THRIVING -> BuddyThriving
    BuddyState.FINE -> BuddyFine
    BuddyState.WORRIED -> BuddyWorried
    BuddyState.SICK -> BuddySick
    BuddyState.FAINTED -> BuddyFainted
}

/** Chip background and the glow behind Buddy. */
fun buddyTint(state: BuddyState): Color = when (state) {
    BuddyState.THRIVING -> BuddyThrivingTint
    BuddyState.FINE -> BuddyFineTint
    BuddyState.WORRIED -> BuddyWorriedTint
    BuddyState.SICK -> BuddySickTint
    BuddyState.FAINTED -> BuddyFaintedTint
}

/** The placeholder's face colour. Thriving keeps the onboarding green. */
internal fun buddyBody(state: BuddyState): Color = when (state) {
    BuddyState.THRIVING -> BuddyGreen
    BuddyState.FINE -> BuddyFineBody
    BuddyState.WORRIED -> BuddyWorriedBody
    BuddyState.SICK -> BuddySickBody
    BuddyState.FAINTED -> BuddyFaintedBody
}
