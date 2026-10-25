package com.example.paisapet.nav

import com.example.paisapet.list.TxFilter
import com.example.paisapet.onboarding.StartScreen

enum class Tab { HOME, TRANSACTIONS }

/** At most one bottom sheet is open. */
sealed interface Sheet {
    data object None : Sheet
    data object Add : Sheet
    data class Edit(val id: Long) : Sheet
    data object Budget : Sheet
}

/** Everything the hand-written navigation remembers. [monthKey] null means "the current month". */
data class NavState(
    val tab: Tab = Tab.HOME,
    val settingsOpen: Boolean = false,
    val debugOpen: Boolean = false,
    val sheet: Sheet = Sheet.None,
    val monthKey: String? = null,
    val filter: TxFilter = TxFilter.ALL,
)

/** Where Back goes: sheet, debug page, Settings, Transactions tab, then null so Android leaves the app. */
fun NavState.back(): NavState? = when {
    sheet != Sheet.None -> copy(sheet = Sheet.None)
    debugOpen -> copy(debugOpen = false)
    settingsOpen -> copy(settingsOpen = false)
    tab == Tab.TRANSACTIONS -> copy(tab = Tab.HOME)
    else -> null
}

/** A row tap or the notification body tap: Edit opens over the current tab. */
fun NavState.openTransaction(id: Long): NavState =
    copy(settingsOpen = false, debugOpen = false, sheet = Sheet.Edit(id))

/** Home's "Tag now". */
fun NavState.tagNow(): NavState = copy(tab = Tab.TRANSACTIONS, filter = TxFilter.NOT_TAGGED)

/** Home's "See all". */
fun NavState.seeAll(): NavState = copy(tab = Tab.TRANSACTIONS, filter = TxFilter.ALL)

/** Only Strings, Booleans, Longs and nulls, so rememberSaveable can store it in a Bundle. */
fun NavState.toSaveable(): List<Any?> {
    val (sheetCode, sheetId) = when (sheet) {
        Sheet.None -> SHEET_NONE to null
        Sheet.Add -> SHEET_ADD to null
        is Sheet.Edit -> SHEET_EDIT to sheet.id
        Sheet.Budget -> SHEET_BUDGET to null
    }
    return listOf(tab.name, settingsOpen, debugOpen, sheetCode, sheetId, monthKey, filter.name)
}

/** The reverse of [toSaveable]; anything unreadable falls back to the default. */
fun navStateFromSaveable(list: List<Any?>): NavState {
    val default = NavState()
    val sheet = when (list.getOrNull(3)) {
        SHEET_ADD -> Sheet.Add
        SHEET_BUDGET -> Sheet.Budget
        SHEET_EDIT -> (list.getOrNull(4) as? Long)?.let { Sheet.Edit(it) } ?: Sheet.None
        else -> Sheet.None
    }
    return NavState(
        tab = Tab.entries.firstOrNull { it.name == list.getOrNull(0) } ?: default.tab,
        settingsOpen = list.getOrNull(1) as? Boolean ?: default.settingsOpen,
        debugOpen = list.getOrNull(2) as? Boolean ?: default.debugOpen,
        sheet = sheet,
        monthKey = list.getOrNull(5) as? String,
        filter = TxFilter.entries.firstOrNull { it.name == list.getOrNull(6) } ?: default.filter,
    )
}

/** What to do with a pending "open this payment" request from the notification body tap. */
enum class StartAction { WAIT, DROP, OPEN }

fun resolveOpen(start: StartScreen): StartAction = when (start) {
    StartScreen.LOADING -> StartAction.WAIT
    StartScreen.ONBOARDING -> StartAction.DROP
    StartScreen.HOME -> StartAction.OPEN
}

private const val SHEET_NONE = "NONE"
private const val SHEET_ADD = "ADD"
private const val SHEET_EDIT = "EDIT"
private const val SHEET_BUDGET = "BUDGET"
