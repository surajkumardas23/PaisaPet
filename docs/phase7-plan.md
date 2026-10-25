# Phase 7 plan: widgets, limit alerts, Buddy art

## Decisions (approved)
- Libraries: `androidx.glance:glance-appwidget:1.2.0`, `androidx.work:work-runtime-ktx:2.12.0`. No other versions.
- Midnight refresh: WorkManager one-time job at next midnight + 1 min, re-queued each run; queued only while a widget is placed.
- Untagged payments: option D, hold back only the payment whose prompt was just shown.
- Keep `ACCESS_NETWORK_STATE` (merged by WorkManager); no `tools:node="remove"`.
- Wide widget budget line: "Budget ₹5,000 for Oct" (short month).
- Tag prompt large icon: not built. No schema change (Room version 1; app/schemas unchanged).
- Phase 7 only: existing tests may change only to adapt to a changed hook signature; behaviour assertions stay.
- No commits by Claude, no connectedAndroidTest, no logging. Stop after each step.

## Build order
| # | Step | Check |
|---|---|---|
| 1 | Add Glance 1.2.0 + work-runtime-ktx 2.12.0; dependency diff, APK sizes, release permissions | `./gradlew test assembleDebug assembleRelease` |
| 2 | `OpenTarget` (Payment / Home / AddExpense) + `NavState.open()` + MainActivity/MainShell | `./gradlew test assembleDebug` |
| 3 | 5 Buddy vectors `res/drawable/buddy_*.xml` + `ui/buddy/BuddyArt.kt` (Home unchanged) | same |
| 4 | `widget/WidgetModel.kt` pure builder + tests | `--tests "com.example.paisapet.widget.*"` |
| 5 | Small 2x2 widget + receiver + provider XML + preview + manifest | `./gradlew test assembleDebug` |
| 6 | `WidgetRefresher` + `AfterDataChange` (widgets only) + hooks + `container.saveBudget` | same |
| 7 | Wide 4x2 widget + "+ Add" | same |
| 9 | `decideLimitAlert` + `limitAlertText` + tests | `--tests "com.example.paisapet.budget.*"` |
| 10 | DAO flag queries + `LimitAlertNotifier` + alerts in `AfterDataChange` with hold-back | `./gradlew test assembleDebug` |
| 8 | `MidnightWorker` + onEnabled/onDisabled + time-change filters (only when asked) | same |
| 11 | Optional: Home draws `buddyDrawable`; CLAUDE.md status; PRD fixes; full run | `./gradlew test lint assembleDebug assembleRelease` |

## Main signatures
```kotlin
// budget/LimitAlerts.kt (pure)
enum class LimitAlert { SICK_75, FAINTED_OVER_100 }
data class AlertFlags(val alerted75: Boolean, val alerted100: Boolean)
data class AlertDecision(val post: LimitAlert?, val flags: AlertFlags)
fun decideLimitAlert(previous: AlertFlags, state: BuddyState): AlertDecision

// notify/LimitAlertText.kt (pure)
data class LimitAlertText(val title: String, val text: String)
fun limitAlertText(alert: LimitAlert, summary: BudgetSummary): LimitAlertText
const val LIMIT_ALERT_ID = 0   // one id: "fainted" replaces "sick"

// widget/WidgetModel.kt (pure, formatting only)
sealed interface WidgetModel {
    data object SetUp : WidgetModel      // before onboarding: "Set up Paisa Pet"
    data object NoBudget : WidgetModel   // summary null
    data class Budgeted(state, amountText, overBudget, ofBudgetText, budgetLine,
                        pctChip, barPercent, lastPayment: String?) : WidgetModel
}
fun buildWidgetModel(onboardingComplete: Boolean, view: MonthView): WidgetModel

// refresh/AfterDataChange.kt: read → decide → write flags → post → refresh, under one Mutex
class AfterDataChange(onboardingComplete, currentMonth, readFlags, writeFlags,
                      postAlert, refreshWidgets, summarizeWithout) {
    suspend fun run(heldBackId: Long? = null)   // never throws except CancellationException
}
class WidgetRefresher(context: Context) { suspend fun refreshIfPlaced() }  // no-op without widgets
```
Widgets always show the current month: one-shot read in `provideGlance` for the first frame, then the same Room flows collected inside `provideContent`.

Wiring (AppContainer):
```kotlin
smsPipeline:      onDataChanged = { id, prompted -> afterChange.run(heldBackId = id.takeIf { prompted }) }
tagActionHandler: onDataChanged = { afterChange.run() }          // awaited inside goAsync
paymentActions:   onDataChanged = { appScope.launch { afterChange.run() } }
suspend fun saveBudget(paise: Long)  // repository.setBudget + afterChange on appScope (Settings, onboarding)
```
Taps: `EXTRA_OPEN_TARGET` "home" / "add" with data `paisapet://open/home|add`, flags NEW_TASK | CLEAR_TOP. Small widget and alert open Home; "+ Add" opens the Add sheet. Dropped during onboarding.

## Alert rules
- reached75 = state is SICK or FAINTED; over100 = state is FAINTED (exact paise comparison already in `buddyStateOf`).
- A flag stays set while its level is reached, and is cleared once % used drops below it (re-arm).
- Over 100 and alerted100 false → post FAINTED, flags (true, true). A jump past both levels posts only this.
- Else reached 75 and alerted75 false → post SICK.
- Exactly 100% is SICK: no "fainted".
- Current month only. Flags written before posting (at most once). POST_NOTIFICATIONS missing → nothing posted, flags still saved, no crash.
- Copy: "Buddy is getting sick" / "₹X misc left this month."; "Buddy fainted" / "You've crossed your ₹Y misc budget. You are ₹Z over budget." Tap opens Home.

## Hold-back rule (option D)
After a new SMS payment whose tag prompt was shown, alerts are decided on the month without that one payment (re-running `summarize()`). Tapping Misc runs a full check (alert can fire); Essential does not fire it. If notifications are off, nothing is held back. A swiped prompt waits for the next change.

## Manifest changes
- `.widget.SmallWidgetReceiver` and `.widget.WideWidgetReceiver`: `exported="true"` (the launcher sends APPWIDGET_UPDATE), filters APPWIDGET_UPDATE, TIME_SET, TIMEZONE_CHANGED; meta-data `android.appwidget.provider` → `@xml/widget_small_info` / `@xml/widget_wide_info`.
- Provider XML: small 110dp × 110dp (2x2), wide 250dp × 110dp (4x2), resize horizontal|vertical, `updatePeriodMillis=0`, home_screen, Glance's default loading layout, `previewLayout` (12+), `previewImage` (8–11), description.
- Merged by libraries: androidx.startup provider, WorkManager services/receivers, Glance trampolines and receivers; permissions WAKE_LOCK, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE, ACCESS_NETWORK_STATE. No INTERNET.

## JVM tests
- LimitAlerts: all states × flag pairs; 74.99/75/100/100.01%; 50→120% jump; re-arm after delete and after budget raise; no repeats.
- LimitAlertText, WidgetModel (SetUp, NoBudget, under/over, bar clamp, last payment per category, empty month, "Oct").
- AfterDataChange with fakes: one alert under concurrency, hold-back, no alert before onboarding or without budget, failures never escape, widgets refreshed every run.
- PaymentActions hook after Done only; OpenTarget parsing and `NavState.open()`; BuddyArt maps every state.

## Phone checks
1. Fresh install, no widget: no Paisa Pet job queued.
2. Widget before onboarding shows "Set up Paisa Pet"; after onboarding, numbers within 5 s.
3. Small widget: place, resize, picker preview, tap → Home (also from Transactions).
4. Wide widget: tap → Home; "+ Add" with app closed, open, on a past month, during onboarding.
5. App swiped away, real UPI payment → widget updates within 5 s.
6. Tag from notification; tag/edit/delete/add in app; budget change → widget updates.
7. Midnight/day change, month rollover via date change, time zone change (step 8).
8. Alerts: 75% once, no repeat, over 100% with amount, re-fire after delete, tap → Home.
9. Hold-back: big payment over 100% → no alert; Essential → none; Misc → alert.
10. Notifications off → no crash, widgets still update.
11. Remove both widgets → midnight job gone.
12. Release APK: widgets work, no INTERNET, no SimulateSms classes.

## PRD fixes for the wrap-up (step 11)
- Alert copy made consistent with the copy above (FR-17 and the widget spec).
- "shortly after midnight" instead of "at midnight" (FR-15, Month rollover).
- Tag prompt large icon = Buddy's state: not built (note it).
- Permission statement: "no INTERNET" instead of "only SMS and notifications" (WorkManager adds harmless permissions).
- Wide widget: "Budget ₹5,000 for Oct".
