# Paisa Pet

See what's left of your misc budget before you tap Pay.

[APK download](TODO) · [Demo video](TODO) · [Install guide](TODO) · [PRD](docs/prd.md) · [Pitch deck](TODO)

## What it is

Paisa Pet is an offline Android app that reads UPI debit SMS on the phone and asks "Misc or Essential?" with one tap from the notification. It shows how much of your monthly misc budget is left through Buddy, a pet whose mood follows your spending, on the Home screen and in two home-screen widgets. There is no backend and no login.

## Screenshots

| Notification | Home | Widgets | Transactions |
| --- | --- | --- | --- |
| ![Notification](docs/screenshots/notification.png) | ![Home](docs/screenshots/home.png) | ![Widgets](docs/screenshots/widgets.png) | ![Transactions](docs/screenshots/transactions.png) |

## Features in v0.1

- Onboarding: welcome, set a monthly misc budget, grant SMS and notification permissions (with a denied state).
- Automatic capture of UPI debit SMS, including UPI AutoPay payments, with duplicate detection.
- One-tap Misc / Essential buttons on the payment notification. Untagged payments count as Misc.
- Home: Buddy, rupees left, % used and recent payments. Buddy has five states (thriving, fine, worried, sick, fainted) at 25 / 50 / 75 / 100% used.
- Transactions list grouped by day, with All / Misc / Essential / Not tagged filters and past months (read-only).
- Edit or delete a payment, and add a cash or missed payment by hand.
- Settings: change the monthly budget, export all payments as CSV through the share sheet.
- Small (2x2) and wide (4x2) home-screen widgets that refresh after every change. The wide widget has an Add button.

## How it works

1. **SMS arrives.** A manifest-registered receiver gets the SMS, even when the app is closed. Multi-part messages are joined.
2. **Parsed on the phone.** The parser runs in order: sender check (personal numbers are dropped), hard rejects (OTP, credit, failed, collect request, mandate set-up and similar), bank templates, then a guarded generic fallback. Templates exist for **HDFC** (regular, AutoPay and payment to a bank account), **SBI** and **ICICI**. The generic fallback captures a message only when all of these hold: the sender looks like a bank ID, the word UPI and exactly one 12-digit reference are present, there is an explicit debit phrase (for example "debited from", "Sent Rs"), an amount can be read, and none of credited, received, refund, reversed, card, ATM, NEFT, IMPS, RTGS, OTP, failed, declined or pending appear. A missed payment is always preferred to a wrong one.
3. **Notification.** The payment is saved as Not tagged and a notification asks "Misc or Essential?".
4. **Tag.** Tapping Misc or Essential tags the payment without opening the app. Tapping the notification body opens the payment's Edit screen.
5. **Home and widgets update.** Home reads the database live, and both widgets refresh after every insert, tag, edit, delete or budget change.

A payment belongs to the month of the date printed in the SMS. Essential payments never count against the budget.

## Privacy

- **No `INTERNET` permission.** The manifest asks only for `RECEIVE_SMS` and `POST_NOTIFICATIONS`. There are no analytics or crash SDKs.
- **No login, no account.**
- **Data stays on the phone** in a local Room database, with settings in DataStore.
- **SMS body and sender are never stored or logged.** Only parsed fields are kept (amount, payee, date, UPI reference, a bank code such as `HDFCBK`) plus a SHA-256 hash of sender and body, used to spot duplicates. The app source has no logging calls.
- **Backup is off** (`android:allowBackup="false"`).

## Install

Android 8.0 (API 26) or newer. Not available for iPhone: iOS does not let apps read SMS.

1. Download the APK from the [APK download](TODO) link.
2. Allow your browser or file app to install unknown apps, then install.
3. On Android 13 and newer the SMS permission may be greyed out. Open the app's page in system Settings, tap the three-dot menu and choose **Allow restricted settings**, then grant SMS.
4. Open the app once after installing, set your budget and grant SMS and notifications.

See the full install guide for Play Protect and other warnings.

## Build from source

Requirements:

- Android Studio [TODO: version used] with Android SDK 37
- JDK 17 or newer (Android Studio's bundled JDK works)
- minSdk 26, targetSdk 37, compileSdk 37

```
git clone https://github.com/surajkumardas23/PaisaPet.git
```

Open the folder in Android Studio, let Gradle sync, connect a phone with USB debugging on, and run the `app` configuration.

Unit tests (JVM, no phone needed):

```
./gradlew test
```

Debug builds include a Simulate SMS screen for testing without real payments; release builds do not.

**Do not run `connectedAndroidTest` on a phone with real payment data.** It uninstalls the app and wipes its data.

## Tech stack

| Area | Library | Version |
| --- | --- | --- |
| Language | Kotlin (coroutines, Flow) | 2.2.10 |
| UI | Jetpack Compose + Material 3 | Compose BOM 2026.02.01 |
| Database | Room (with KSP) | 2.8.5 (KSP 2.3.12) |
| Settings | DataStore Preferences | 1.3.0 |
| Widgets | Glance (glance-appwidget) | 1.2.0 |
| Background | WorkManager | 2.12.0 |
| Build | Android Gradle Plugin / Gradle | 9.4.1 / 9.6.0 |
| Tests | JUnit | 4.13.2 |

No dependency injection library (a hand-written `AppContainer`) and no navigation library.

## Project structure

```
app/src/main/java/com/example/paisapet/
├── sms/          SMS receiver, parser, bank templates, generic fallback, de-duplication
├── budget/       Pure budget maths: misc spent, left, % used, Buddy state, month keys
├── data/         Room database, DAOs, repository, DataStore settings
├── notify/       Tag notification, Misc/Essential action receiver and handler
├── edit/         Rules for tagging, editing, deleting and adding payments
├── list/         Day grouping, filters and month options for Transactions
├── export/       CSV export (with spreadsheet formula neutralisation)
├── widget/       Glance small and wide widgets and their pure model
├── refresh/      Widget refresh after every data change
├── nav/          Hand-written navigation state and back rules
├── onboarding/   Start-up gate and onboarding logic
├── format/       Rupee formatting with Indian grouping
├── time/         Clock and midnight helpers
└── ui/           Compose screens: home, transactions, edit, settings, onboarding, theme, buddy
app/src/debug/    Simulate SMS screen (debug builds only)
docs/             PRD, SMS fixtures (sample-sms.txt), screen mockups
```

## Limitations and what's next

Limitations:

- UPI debits only. Card, ATM, NEFT/IMPS, NACH, credits and failed payments are ignored.
- Only HDFC, SBI and ICICI have bank templates. Other banks rely on the guarded fallback until real SMS samples are collected.
- Android only, sideloaded APK. No iPhone and no Play Store (Google restricts SMS apps).
- No back-fill: payments made before install are not read; add them by hand.
- One budget, two categories (Misc and Essential). No cloud backup or sync.

Next (v0.2):

- 75% and 100% limit alerts.
- More banks (Axis, Kotak and others) from tester SMS samples.
- Undo after tagging.
- Midnight widget refresh job, quick-add chips.
- Dark theme and real Buddy art.

## Built by

Suraj Das, HelloPM AI Buildathon, October 2026. Built with Claude Code.
