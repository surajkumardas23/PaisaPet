package com.example.paisapet.ui.buddy

import com.example.paisapet.budget.BuddyState

// The one place for Buddy's per-state text. Phase 5 step 4 adds the colours here; Phase 7 replaces the art.

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
