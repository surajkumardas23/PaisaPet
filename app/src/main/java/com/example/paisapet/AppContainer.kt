package com.example.paisapet

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.paisapet.budget.monthKeyOf
import com.example.paisapet.data.TransactionRepository
import com.example.paisapet.data.db.PaisaPetDatabase
import com.example.paisapet.data.settings.SettingsStore
import com.example.paisapet.edit.PaymentActions
import com.example.paisapet.notify.TagActionHandler
import com.example.paisapet.notify.TagPromptNotifier
import com.example.paisapet.refresh.AfterDataChange
import com.example.paisapet.sms.SmsParser
import com.example.paisapet.sms.SmsPipeline
import com.example.paisapet.time.SystemClock
import com.example.paisapet.widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId

/** Hand-written dependency container (no Hilt). Everything is lazy so app start stays fast. */
class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    val database: PaisaPetDatabase by lazy { PaisaPetDatabase.create(applicationContext) }

    // The only place that may create the "settings" DataStore: two instances on one file crash the app.
    private val settingsDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { applicationContext.preferencesDataStoreFile("settings") },
        )
    }

    val settings: SettingsStore by lazy { SettingsStore(settingsDataStore) }

    /**
     * The one clock for the repository, the SMS pipeline and the read-only check, so all agree on the current month.
     * Its zone follows the phone's time zone at every read.
     */
    val clock: Clock by lazy { SystemClock() }

    /** Read at the moment of use; never keep it in a val. */
    val zone: ZoneId get() = clock.zone

    val repository: TransactionRepository by lazy {
        TransactionRepository(database, settings, clock)
    }

    /** Receivers launch their goAsync() work here, off the main thread. */
    val appScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    val tagPromptNotifier: TagPromptNotifier by lazy { TagPromptNotifier(applicationContext) }

    val widgetRefresher: WidgetRefresher by lazy { WidgetRefresher(applicationContext) }

    /** Runs after every data change: widgets now, limit alerts in step 10. */
    val afterChange: AfterDataChange by lazy {
        AfterDataChange(refreshWidgets = { widgetRefresher.refreshIfPlaced() })
    }

    /** For changes made from the UI: on appScope, so leaving the screen cannot cancel it. */
    fun launchAfterChange() {
        appScope.launch { afterChange.run() }
    }

    // Receivers await these hooks inside goAsync(), so the refresh finishes before the receiver does.
    val smsPipeline: SmsPipeline by lazy {
        SmsPipeline(
            parse = { s, b, t -> SmsParser.parse(s, b, t, zone) },
            save = repository::saveParsedDebit,
            prompt = { id, debit -> tagPromptNotifier.show(id, debit) },
            onDataChanged = { _, _ -> afterChange.run() },
        )
    }

    val tagActionHandler: TagActionHandler by lazy {
        TagActionHandler(
            setCategory = repository::setCategory,
            cancelPrompt = tagPromptNotifier::cancel,
            onDataChanged = { afterChange.run() },
        )
    }

    /** In-app tag, edit, delete and add (S7, S8). */
    val paymentActions: PaymentActions by lazy {
        PaymentActions(
            store = repository,
            cancelPrompt = tagPromptNotifier::cancel,
            currentMonthKey = { monthKeyOf(clock.instant(), zone) },
            onDataChanged = { launchAfterChange() },
        )
    }

    /** The budget from onboarding (S2) and Settings (S9): repository.setBudget, then the widgets and alerts. */
    suspend fun saveBudget(paise: Long) {
        repository.setBudget(paise)
        launchAfterChange()
    }
}
