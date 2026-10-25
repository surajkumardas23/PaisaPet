package com.example.paisapet.nav

import com.example.paisapet.list.TxFilter
import com.example.paisapet.onboarding.StartScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainNavTest {

    // Back order

    @Test fun backFromHomeLeavesTheApp() {
        assertNull(NavState().back())
    }

    @Test fun backFromTransactionsGoesHome() {
        assertEquals(NavState(tab = Tab.HOME), NavState(tab = Tab.TRANSACTIONS).back())
    }

    @Test fun backClosesSheetFirst() {
        val s = NavState(tab = Tab.TRANSACTIONS, settingsOpen = true, sheet = Sheet.Edit(4))
        assertEquals(s.copy(sheet = Sheet.None), s.back())
    }

    @Test fun backOrderDebugThenSettingsThenHome() {
        val start = NavState(tab = Tab.TRANSACTIONS, settingsOpen = true, debugOpen = true)
        val afterDebug = start.back()!!
        assertEquals(start.copy(debugOpen = false), afterDebug)
        val afterSettings = afterDebug.back()!!
        assertEquals(afterDebug.copy(settingsOpen = false), afterSettings)
        assertEquals(Tab.HOME, afterSettings.back()!!.tab)
        assertNull(afterSettings.back()!!.back())
    }

    @Test fun backKeepsTheSelectedMonthAndFilter() {
        val s = NavState(tab = Tab.TRANSACTIONS, monthKey = "2026-09", filter = TxFilter.ESSENTIAL)
        val back = s.back()!!
        assertEquals("2026-09", back.monthKey)
        assertEquals(TxFilter.ESSENTIAL, back.filter)
    }

    // Actions

    @Test fun openTransactionClosesSettingsAndDebug() {
        val s = NavState(tab = Tab.TRANSACTIONS, settingsOpen = true, debugOpen = true, sheet = Sheet.Add)
        assertEquals(NavState(tab = Tab.TRANSACTIONS, sheet = Sheet.Edit(9)), s.openTransaction(9))
    }

    @Test fun tagNowOpensNotTagged() {
        val s = NavState().tagNow()
        assertEquals(Tab.TRANSACTIONS, s.tab)
        assertEquals(TxFilter.NOT_TAGGED, s.filter)
    }

    @Test fun seeAllOpensAll() {
        val s = NavState(filter = TxFilter.MISC).seeAll()
        assertEquals(Tab.TRANSACTIONS, s.tab)
        assertEquals(TxFilter.ALL, s.filter)
    }

    // Save and restore

    @Test fun roundTripEverySheet() {
        for (sheet in listOf(Sheet.None, Sheet.Add, Sheet.Edit(123L), Sheet.Budget)) {
            val s = NavState(tab = Tab.TRANSACTIONS, settingsOpen = true, debugOpen = true, sheet = sheet,
                monthKey = "2026-09", filter = TxFilter.NOT_TAGGED)
            assertEquals(s, navStateFromSaveable(s.toSaveable()))
        }
    }

    @Test fun roundTripNullMonth() {
        val s = NavState(monthKey = null)
        assertEquals(s, navStateFromSaveable(s.toSaveable()))
    }

    @Test fun saveableHoldsOnlyBundleSafeValues() {
        val values = NavState(sheet = Sheet.Edit(5), monthKey = "2026-10").toSaveable()
        assertTrue(values.all { it == null || it is String || it is Boolean || it is Long })
    }

    @Test fun unreadableSaveableFallsBackToDefaults() {
        assertEquals(NavState(), navStateFromSaveable(emptyList()))
        assertEquals(NavState(), navStateFromSaveable(listOf("NOPE", "x", 3, "EDIT", "notALong", 7, "BAD")))
    }

    // Notification tap

    @Test fun resolveOpenForEachStartScreen() {
        assertEquals(StartAction.WAIT, resolveOpen(StartScreen.LOADING))
        assertEquals(StartAction.DROP, resolveOpen(StartScreen.ONBOARDING))
        assertEquals(StartAction.OPEN, resolveOpen(StartScreen.HOME))
    }

    // Notification body tap (step 8)

    @Test fun openTransactionReplacesAnyPreviousSheet() {
        for (previous in listOf(Sheet.Add, Sheet.Budget, Sheet.Edit(3))) {
            assertEquals(Sheet.Edit(9), NavState(sheet = previous).openTransaction(9).sheet)
        }
    }

    @Test fun openTransactionFromSettingsAndDebugKeepsTabMonthAndFilter() {
        val s = NavState(tab = Tab.TRANSACTIONS, settingsOpen = true, debugOpen = true, monthKey = "2026-09", filter = TxFilter.MISC)
        val opened = s.openTransaction(12)
        assertEquals(NavState(tab = Tab.TRANSACTIONS, sheet = Sheet.Edit(12), monthKey = "2026-09", filter = TxFilter.MISC), opened)
        // Back from the opened sheet goes to the tab, not back into Settings.
        assertEquals(NavState(tab = Tab.TRANSACTIONS, monthKey = "2026-09", filter = TxFilter.MISC), opened.back())
    }

    @Test fun openTransactionSurvivesSaveAndRestore() {
        val opened = NavState(settingsOpen = true).openTransaction(Long.MAX_VALUE)
        assertEquals(opened, navStateFromSaveable(opened.toSaveable()))
    }

    // Widget and alert taps (Phase 7)

    private val home = NavState(tab = Tab.HOME, sheet = Sheet.None, monthKey = null, filter = TxFilter.ALL)

    @Test fun openHomeFromSettings() {
        assertEquals(home, NavState(tab = Tab.TRANSACTIONS, settingsOpen = true).open(OpenTarget.Home))
    }

    @Test fun openHomeFromTheDebugPage() {
        assertEquals(home, NavState(settingsOpen = true, debugOpen = true).open(OpenTarget.Home))
    }

    @Test fun openHomeFromAPastMonthGoesToTheCurrentMonth() {
        val s = NavState(tab = Tab.TRANSACTIONS, monthKey = "2026-09", filter = TxFilter.NOT_TAGGED)
        assertEquals(home, s.open(OpenTarget.Home))
    }

    @Test fun openHomeClosesAnySheet() {
        for (sheet in listOf(Sheet.Add, Sheet.Budget, Sheet.Edit(3))) {
            assertEquals(home, NavState(sheet = sheet).open(OpenTarget.Home))
        }
    }

    @Test fun openHomeThenBackLeavesTheApp() {
        assertNull(NavState(tab = Tab.TRANSACTIONS, settingsOpen = true).open(OpenTarget.Home).back())
    }

    @Test fun openAddFromTransactionsInAPastMonth() {
        val s = NavState(tab = Tab.TRANSACTIONS, monthKey = "2026-09", filter = TxFilter.MISC)
        assertEquals(home.copy(sheet = Sheet.Add), s.open(OpenTarget.AddExpense))
    }

    @Test fun openAddFromSettingsDebugAndAnotherSheet() {
        for (sheet in listOf(Sheet.Budget, Sheet.Edit(3))) {
            val s = NavState(settingsOpen = true, debugOpen = true, sheet = sheet)
            assertEquals(home.copy(sheet = Sheet.Add), s.open(OpenTarget.AddExpense))
        }
    }

    @Test fun openAddThenBackShowsHome() {
        assertEquals(home, NavState(tab = Tab.TRANSACTIONS).open(OpenTarget.AddExpense).back())
    }

    @Test fun openPaymentIsOpenTransaction() {
        val states = listOf(
            NavState(),
            NavState(tab = Tab.TRANSACTIONS, settingsOpen = true, debugOpen = true, sheet = Sheet.Add),
            NavState(tab = Tab.TRANSACTIONS, monthKey = "2026-09", filter = TxFilter.MISC, sheet = Sheet.Budget),
        )
        for (s in states) {
            assertEquals(s.openTransaction(12), s.open(OpenTarget.Payment(12)))
        }
    }

    @Test fun openedTargetsSurviveSaveAndRestore() {
        for (target in listOf(OpenTarget.Home, OpenTarget.AddExpense, OpenTarget.Payment(5))) {
            val opened = NavState(settingsOpen = true, monthKey = "2026-09").open(target)
            assertEquals(opened, navStateFromSaveable(opened.toSaveable()))
        }
    }
}
