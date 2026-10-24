# Phase 2 plan: what is left (steps 5–9)

Phase 2 = everything behind the screens, no UI (FR-08, FR-09, FR-10, FR-16, NFR-08).
Steps 1–4 are done. This file is enough to continue in a fresh session.

## Already built (steps 1–4)
- Dependencies in `gradle/libs.versions.toml`: KSP 2.3.12, Room 2.8.5 (runtime, ktx, compiler, Gradle plugin), DataStore 1.3.0 (preferences). Room schema goes to `app/schemas` (not git-ignored).
- `model/Enums.kt`: `Category { MISC, ESSENTIAL, UNTAGGED }`, `Source { SMS, MANUAL }`, `TagSource { NOTIFICATION, APP }`.
- `budget/MonthKeys.kt`: `monthKeyOf`, `monthStart`, `monthEndExclusive`, `daysLeft`, `parseMonthKey`.
- `budget/BuddyState.kt`, `budget/BudgetMath.kt`: `buddyStateOf`, `Spend`, `BudgetSummary`, `miscSpent`, `left`, `pctUsedFloor`, `pacePerDay`, `effectiveBudget`, `summarize`. 69 JVM tests in `com.example.paisapet.budget`.
- `data/db/`: `TransactionEntity`, `MonthBudgetEntity`, `TransactionDao` (`insert` IGNORE → Long, `observeByMonth`, `findDuplicateCandidates`, `updateCategory` → Int), `MonthBudgetDao` (`observe`, `insertIfMissing` IGNORE), `PaisaPetDatabase` (version 1, exportSchema, `create(context)`).

## Decisions (fixed)
- Manual rows are excluded from the duplicate check (the candidates query has `source = 'SMS'`).
- `monthKey` is fixed when the row is saved, and later time-zone changes don't move it.
- No budget before onboarding: `defaultBudgetPaise` is null and the summary is null. The maths requires budget > 0.
- Pace rounds down to whole paise.
- `setCategory` accepts MISC, ESSENTIAL and also UNTAGGED (needed for Undo, FR-21). UNTAGGED clears `tagSource` and `taggedAt`.
- Never `fallbackToDestructiveMigration` (NFR-08). No Room 3 (`androidx.room3`).
- Not built this phase: the midnight WorkManager job, alert flag logic (FR-17), budget editing, widget refresh. The `alerted75/100` columns already exist.
- If a dependency version fails to resolve, stop and ask. Don't pick other versions or raise Kotlin.
- At the end of the phase the PRD "Data model" row for `smsHash` must say "SHA-256 of sender + newline + body". The user gives the exact edit.

## Working rules for these steps
- One step at a time. After each step: say what ran and its results, then stop and wait.
- No commit until step 9.

## Step 5: SettingsStore + JVM test
- `data/settings/SettingsStore.kt`, constructor `SettingsStore(dataStore: DataStore<Preferences>)`:
  - `onboardingComplete: Flow<Boolean>` (false when unset)
  - `defaultBudgetPaise: Flow<Long?>` (null when unset)
  - `suspend fun setOnboardingComplete(Boolean)`, `suspend fun setDefaultBudgetPaise(Long)` (require > 0)
- `src/test/.../data/settings/SettingsStoreTest.kt` (JVM): create the store with `PreferenceDataStoreFactory.create { tempFolder.newFile("t.preferences_pb") }`, using `runBlocking` and `first()`. Check the defaults (false, null) and that each setter round-trips.
- If the DataStore JVM test can't run on the laptop, tell the user **before** moving it to androidTest.
- Run: `./gradlew :app:testDebugUnitTest --tests "com.example.paisapet.data.settings.*"`

## Step 6: Deduplicator.WINDOW public + repository
- `sms/Deduplicator.kt`: change `private val WINDOW` to public. That is the only change in `sms/`.
- `data/TransactionRepository.kt`, constructor `(db: PaisaPetDatabase, settings: SettingsStore, clock: java.time.Clock)`. `zone = clock.zone`.
  ```kotlin
  sealed interface SaveResult { data class Saved(val id: Long) : SaveResult; data object Duplicate : SaveResult }
  data class MonthView(val transactions: List<TransactionEntity>, val summary: BudgetSummary?)
  suspend fun saveParsedDebit(debit: ParsedDebit): SaveResult
  suspend fun setCategory(id: Long, category: Category, tagSource: TagSource): Boolean
  fun observeMonth(monthKey: String, today: LocalDate): Flow<MonthView>
  ```
- **saveParsedDebit** runs entirely inside `db.withTransaction { }`:
  1. `now = clock.millis()`.
  2. `dao.findDuplicateCandidates(smsHash, upiRef, amountPaise, occurredAt − WINDOW, occurredAt + WINDOW)`, in epoch ms.
  3. Map each candidate row to a `ParsedDebit` (`Instant.ofEpochMilli`, with `dateFromMessage = true`, which dedup ignores).
  4. If `Deduplicator.isDuplicate(debit, candidates)`, return `Duplicate`.
  5. Insert the debit's fields with `category = UNTAGGED`, `source = SMS`, `tagSource = null`, `taggedAt = null` and `createdAt = updatedAt = now`. If insert returns −1 (smsHash conflict), return `Duplicate`.
  6. If a default budget exists, `monthBudgetDao.insertIfMissing(MonthBudgetEntity(debit.monthKey, default))`. It never overwrites, and creates nothing if there is no default.
  7. Return `Saved(id)`.
- **setCategory**:
  - MISC or ESSENTIAL: `updateCategory(id, category, tagSource, taggedAt = now, updatedAt = now)`.
  - UNTAGGED: `updateCategory(id, UNTAGGED, null, null, now)`.
  - Returns `rows > 0`, so false means the id is gone and nothing happened. It never touches `monthKey` or `occurredAt`.
- **observeMonth** combines `dao.observeByMonth(monthKey)`, `monthBudgetDao.observe(monthKey)` and `settings.defaultBudgetPaise`:
  - `budget = effectiveBudget(row?.budgetPaise, default)`
  - `summary = budget?.let { summarize(monthKey, it, rows.map { Spend(it.amountPaise, it.category) }, today) }`
- Run: `./gradlew test assembleDebug`

## Step 7: all phone tests, written together, ONE `connectedAndroidTest` run
In-memory Room (`Room.inMemoryDatabaseBuilder`), `runBlocking`, `Flow.first()`, and a fixed `Clock` with zone Asia/Kolkata. No new test libraries.

**`data/db/TransactionDaoTest`**
- Insert and read back every field. Enums round-trip.
- The same smsHash inserted twice: the second insert returns −1. Two manual rows with a null smsHash are both kept.
- `observeByMonth`:
  - It returns only that month, newest first.
  - It emits again after an insert and after `updateCategory`.
- `findDuplicateCandidates`:
  - It finds rows by hash, by upiRef, and by the same amount at ±120 s.
  - It does not return a row at 121 s, a row with a different amount, or a MANUAL row.
- `updateCategory` on an unknown id returns 0.

**`data/db/MonthBudgetDaoTest`**
- `observe` emits null for a missing month and the row once it is inserted.
- `insertIfMissing` doesn't overwrite an existing row and returns −1.

**`data/TransactionRepositoryTest`**
- A saved debit is UNTAGGED and SMS, with null tagSource and taggedAt, and createdAt = updatedAt = clock time.
- Duplicates:
  - The same smsHash gives `Duplicate` and the count stays 1.
  - The same upiRef with a different hash gives `Duplicate`.
  - Different upiRefs with the same amount in the same minute are both saved.
  - No upiRef, with the same amount and payee within 2 min, gives `Duplicate`.
- Month budget row: it is created from the default when missing. An existing row is not overwritten, and nothing is created with no default.
- `setCategory`:
  - MISC or ESSENTIAL sets tagSource and taggedAt.
  - UNTAGGED clears both.
  - An unknown id returns false.
  - A payment on 31 Oct 2026 23:59 IST tagged at 1 Nov 00:05 stays in "2026-10".
- A payment on 31 Oct 23:59 IST and one on 1 Nov 00:00 IST land in different months.
- `observeMonth`:
  - It counts UNTAGGED + MISC but not ESSENTIAL.
  - The budget comes from the row, otherwise from the default.
  - Changing the default updates a month that has no row.
  - The summary is null when there is no budget at all.
- Run (phone plugged in, USB debugging on): `./gradlew connectedAndroidTest`

## Step 8: wiring
- `AppContainer.kt`: a hand-written class taking `(context: Context)`. It holds `PaisaPetDatabase.create(context)`, the `SettingsStore` (DataStore via `preferencesDataStore("settings")`), `Clock.systemDefaultZone()` and the `TransactionRepository`, all lazy. No Hilt.
- `PaisaPetApp.kt`: an `Application` that creates `container` in `onCreate`.
- `AndroidManifest.xml`: `android:name=".PaisaPetApp"` on `<application>`. No new permissions.
- Run: `./gradlew test assembleDebug lint`, then install from Android Studio and check that the app opens.

## Step 9: finish
- Run `./gradlew test assembleDebug`.
- Confirm a search for `fallbackToDestructiveMigration` finds nothing.
- Confirm the schema JSON is tracked.
- Apply the PRD `smsHash` edit (the user gives the text).
- Update the CLAUDE.md "Current status".
- Commit: "Phase 2: Room, DataStore, repository, budget maths and Buddy state". Never push.
