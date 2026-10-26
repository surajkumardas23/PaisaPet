# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Paisa Pet: an offline, single-activity Android app (Kotlin, Jetpack Compose, Material 3) that reads UPI debit SMS, asks "Misc or Essential?" with a one-tap notification, and shows the remaining monthly misc budget through a pet called Buddy (home screen and Glance widgets). No backend, no login.

`docs/prd.md` is the spec and the source of truth. Requirements have IDs (US-xx, FR-xx, NFR-xx) and the work is split into build phases (see "Build phases for Claude Code" in the PRD). Don't read the whole PRD at once. Grep for the `## ` headings and read only the sections you need. Screen mockups are in `docs/screens/` (`docs/screens/*.png`). `05-home-dark.png` is for the later dark theme (P1); ignore it until I ask. **Where a screenshot and the PRD disagree, the PRD wins.** Each screen's section in "Screens and UI spec" lists its changes.

The package is `com.example.paisapet`.

## Working rules

- Work one phase at a time. Read only the current phase in the PRD, build it, then stop. Don't start the next phase until I say so.
- When building UI, look only at the screenshots for the screen you are building.
- Ask before adding a library or changing Gradle versions. Dependencies go through `gradle/libs.versions.toml`.
- Keep explanations brief. After each step, say what changed and which files.
- Run `test` and `assembleDebug` before saying a phase is done.
- Commit after each working phase with a clear message. Never force push. I handle `git push` and sign-in myself.

## Current status

- Phase 0: project created, runs on phone, repo on GitHub. Done.
- Phase 0.5: screenshots moved to docs/screens/, minSdk set to 26, PRD tidy-ups. Done.
- Phase 1: SMS parser, duplicate rules and fixture tests in com.example.paisapet.sms. 95 tests green. Done.
- Phase 2: Room database, repository, SettingsStore, budget maths and Buddy state. 179 JVM tests and 37 phone tests green. Done.
- Phase 3: SMS receiver, tag notification and tag actions, guarded fallback and debug Simulate SMS. 337 JVM tests green; verified on my phone with real HDFC payments. Done.
- Phase 4: onboarding screens, theme, start-up gate and budget saving. Done.
- Phase 5: navigation shell, Home, Transactions, Edit, Add, Settings with budget editing, past months (read-only), notification body tap and CSV export. 636 JVM tests and 80 phone tests green; verified on my phone. Done.
- Phase 6/7: release readiness (version 0.1, .gitignore protects keystores and APKs), small and wide widgets with live refresh. 713 JVM tests and 80 phone tests green. Deferred to v0.2: limit alerts (FR-17), the midnight WorkManager job, Undo, quick-add chips, dark theme, real Buddy art. Next: sign the APK and send it to testers; then iterate (bank templates from tester samples).

Update this section at the end of every phase.

## Commands

On Windows, use `gradlew.bat` from PowerShell or `./gradlew` from Bash.

```
./gradlew assembleDebug                 # build debug APK
./gradlew test                          # JVM unit tests (parser, budget maths)
./gradlew :app:testDebugUnitTest --tests "com.example.paisapet.SomeTest"           # one test class
./gradlew :app:testDebugUnitTest --tests "com.example.paisapet.SomeTest.someCase"  # one test method
./gradlew lint                          # Android lint
./gradlew connectedAndroidTest          # instrumented tests (needs a connected phone)
```

There may be no emulator (the dev laptop is Windows on ARM), so plan to test on a physical phone. Run `test` and `assembleDebug` to check your own work. The user installs on the phone from Android Studio.

## Hard rules

- **UPI debits only.** Card, ATM, NEFT/IMPS, NACH, credits, OTPs, failed payments, collect requests and mandate set-ups must produce no transaction. UPI AutoPay payments that report money sent count as UPI debits. Mandate set-up and pre-debit notices do not. Classification and extraction rules are in the PRD under "SMS capture and parsing rules".
- **No `INTERNET` permission**, no analytics or crash SDKs, no network calls. Never store raw SMS bodies, and never log them in release builds. Store only the parsed fields plus a SHA-256 `smsHash` of sender + body.
- **Money is a `Long` in paise.** Use no floating point for amounts. Display Indian grouping (₹1,00,000) and hide paise unless non-zero.
- Untagged transactions count as Misc. Essential never counts against the budget. A transaction belongs to the month of `occurredAt` (local time zone), not the month it was tagged. occurredAt is the date printed in the SMS, with time of day from the SMS timestamp (fallback: the SMS timestamp).
- Where the PRD's parsing rules disagree with a fixture in docs/sample-sms.txt, the fixture wins. Tell me so the PRD can be fixed.
- No destructive Room migrations once testers have installed the app (NFR-08).
- The "Simulate SMS" tool (FR-19) exists only in debug builds.

## Architecture gotchas

Full design is in the PRD ("Data model", "Budget logic, Buddy states and alerts", "SMS capture and parsing rules"). Only the easy-to-get-wrong points are repeated here.

- `SmsParser` is pure Kotlin with no Android dependencies. Write the fixtures in `docs/sample-sms.txt` before the parser.
- After the SMS receiver gets a message, all work runs off the main thread (`goAsync()` + coroutine, or expedited WorkManager). Debug Simulate SMS feeds the same pipeline.
- Budget values (miscSpent, left, pctUsed, pace, Buddy state) come from pure functions, tested at the boundaries (24.99/25/50/75/100/100.01%).
- The UI observes Room `Flow`s. No Hilt: use a hand-written `AppContainer`.
- Notification ID is the transaction ID. Misc and Essential actions use immutable PendingIntents to a `TagActionReceiver`.
- Refresh Glance widgets after every insert, tag, edit, delete or budget change, and from a daily WorkManager job just after midnight.
- Parser templates exist only for HDFC, SBI and ICICI; a new bank needs real samples in docs/sample-sms.txt first.
- smsHash is the SHA-256 of sender, then a newline character, then body. Never change this once testers install.
- Parser tests read ../docs/sample-sms.txt; run them with ./gradlew :app:testDebugUnitTest --tests com.example.paisapet.sms.*
- Only AppContainer creates the settings DataStore; two DataStore instances on one file crash the app.
- A month's budget is its month_budgets row if there is one, otherwise the default budget (SettingsStore).
- connectedAndroidTest uninstalls the app from the phone and wipes its data: never run it on a phone holding real payment data. Room tests run on the phone: ./gradlew connectedAndroidTest.
- Database changes: bump the Room version, write a real Migration, commit the new schema JSON in app/schemas. Never use destructive migration.
- Parsed money, months and Buddy state come from the pure functions in budget/ (month filed by occurredAt); do not recompute them in UI code.
- Receivers are thin: SmsReceiver and TagActionReceiver use goAsync() on appScope, catch Throwable and call finish() in finally. The logic is in SmsPipeline and TagActionHandler (pure Kotlin, tested).
- Parser order: sender check, hard rejects, bank templates, guarded generic fallback (viaFallback is not stored), explain the ignore. For a new bank wording, add a real redacted fixture to docs/sample-sms.txt first, then a template.
- Simulate SMS and ReceiverTrace live only in src/debug; src/release has stubs. After changes, check that the release APK has no SimulateSms or SampleSms classes. The debug screen is MainActivity for now; Phase 5 replaces it.
- The notification body tap passes TagPromptNotifier.EXTRA_OPEN_TRANSACTION_ID to MainActivity, which ignores it until Phase 5, when it must open the Edit screen (S7) for that payment.
- allowBackup is false on purpose (data stays on the phone). Never log or store an SMS body or sender, and never put them in notification extras.
- Start-up gate: AppRoot reads SettingsStore.onboardingComplete (null = loading, shows a blank screen in the app background). Screens never navigate to Home directly: they set the flag and the gate switches. The debug Reset onboarding button sets it back to false and keeps all data.
- Navigation is hand-written (rememberSaveable step + BackHandler); no navigation library. Phase 5 decides its own approach at its plan.
- Budget saving: repository.setBudget(paise) writes the DataStore default and the current month's month_budgets row (updateBudget keeps alerted75/alerted100). Settings reuses it.
- Theme: colours are named constants in ui/theme/Color.kt; dynamic colour is off; light scheme only. Buddy is drawn ONLY in ui/buddy/BuddyPlaceholder.kt (Phase 7 replaces it). Icons are copied vector drawables in res/drawable/ic_*.xml; material-icons-core is NOT a dependency.
- Navigation is hand-written: one NavState (tab, settings, debug page, sheet, selected month, filter) in nav/MainNav.kt with pure back rules and a Saver. Home, Transactions, Edit and Add read data only through repository flows and pure builders (HomeModel, TransactionsModel); no budget maths in UI code.
- Past months are read-only everywhere in the app (PaymentActions returns ReadOnly); only the notification's own Misc/Essential buttons can still tag a late payment.
- PaymentActions is the only place that tags, edits or deletes from the UI and it cancels the payment's notification after a successful write.
- The notification body tap passes EXTRA_OPEN_TRANSACTION_ID; MainActivity is singleTop and hands an OpenRequest(id, seq) to AppRoot, which applies it once the start screen is HOME and drops it during onboarding. Each notification's open-app PendingIntent is unique per payment.
- The app clock follows the system zone at the moment of use (time/SystemClock); do not cache a ZoneId.
- CSV export: share sheet + FileProvider; the only manifest addition for it is the provider. Keep formula neutralisation in export/CsvExport.kt.
- Never run connectedAndroidTest on a phone with real payment data; it uninstalls the app.
- Widgets: Glance. SmallWidget and WideWidget read through widget/WidgetData.kt (one-shot first frame, then collect the same flow inside provideContent); content comes only from the pure widget/WidgetModel.kt. The refresh path is AfterDataChange (one Mutex) -> WidgetRefresher.refreshIfPlaced (no-op with no widget). To add alerts later, extend AfterDataChange (constructor parameters with defaults) rather than adding new hooks.
- Widget taps use openAppIntent(context, target) with target 'home' or 'add' and a unique data URI; MainActivity turns them into OpenRequest(OpenTarget.Home / AddExpense); NavState.open(target) applies them; the gate drops them during onboarding.
- Release builds: R8 is on and works with Glance and WorkManager without keep-rules; always run assembleRelease and check the release APK after adding a library. Never commit a keystore or an APK.