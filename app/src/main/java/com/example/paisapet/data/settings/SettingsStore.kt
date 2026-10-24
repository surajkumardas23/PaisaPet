package com.example.paisapet.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** App settings in Preferences DataStore. The DataStore itself is built elsewhere (AppContainer). */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    /** False until onboarding has been finished. */
    val onboardingComplete: Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }

    /** Null until a budget has been set (no budget before onboarding). */
    val defaultBudgetPaise: Flow<Long?> =
        dataStore.data.map { it[DEFAULT_BUDGET_PAISE] }

    suspend fun setOnboardingComplete(value: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETE] = value }
    }

    /** Throws IllegalArgumentException unless [value] > 0. */
    suspend fun setDefaultBudgetPaise(value: Long) {
        require(value > 0) { "Budget must be > 0 paise, was $value" }
        dataStore.edit { it[DEFAULT_BUDGET_PAISE] = value }
    }

    private companion object {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboardingComplete")
        val DEFAULT_BUDGET_PAISE = longPreferencesKey("defaultBudgetPaise")
    }
}
