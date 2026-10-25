package com.example.paisapet.ui.buddy

import androidx.annotation.DrawableRes
import com.example.paisapet.R
import com.example.paisapet.budget.BuddyState

// The only mapping from Buddy's state to a picture, for widgets and notification icons.
// The five res/drawable/buddy_*.xml files are placeholders; real art replaces those files, not this code.

@DrawableRes
fun buddyDrawable(state: BuddyState): Int = when (state) {
    BuddyState.THRIVING -> R.drawable.buddy_thriving
    BuddyState.FINE -> R.drawable.buddy_fine
    BuddyState.WORRIED -> R.drawable.buddy_worried
    BuddyState.SICK -> R.drawable.buddy_sick
    BuddyState.FAINTED -> R.drawable.buddy_fainted
}
