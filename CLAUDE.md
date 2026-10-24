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
- Phase 1: not started.

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