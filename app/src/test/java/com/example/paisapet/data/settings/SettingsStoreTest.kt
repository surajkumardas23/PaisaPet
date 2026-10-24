package com.example.paisapet.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsStoreTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val jobs = mutableListOf<Job>()

    private val file: File by lazy { File(tempFolder.root, "t.preferences_pb") }

    private fun newDataStore(): Pair<DataStore<Preferences>, Job> {
        val job = Job()
        jobs += job
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file },
        )
        return dataStore to job
    }

    private fun newStore(): Pair<SettingsStore, Job> {
        val (dataStore, job) = newDataStore()
        return SettingsStore(dataStore) to job
    }

    @After fun cancelScopes() = runBlocking { jobs.forEach { it.cancelAndJoin() } }

    @Test fun defaults() = runBlocking {
        val (store, _) = newStore()
        assertFalse(store.onboardingComplete.first())
        assertNull(store.defaultBudgetPaise.first())
    }

    @Test fun onboardingRoundTrips() = runBlocking {
        val (store, _) = newStore()
        store.setOnboardingComplete(true)
        assertTrue(store.onboardingComplete.first())
    }

    @Test fun onboardingTrueThenFalse() = runBlocking {
        val (store, _) = newStore()
        store.setOnboardingComplete(true)
        store.setOnboardingComplete(false)
        assertFalse(store.onboardingComplete.first())
    }

    @Test fun budgetRoundTrips() = runBlocking {
        val (store, _) = newStore()
        store.setDefaultBudgetPaise(5_000_00L)
        assertEquals(5_000_00L, store.defaultBudgetPaise.first())
    }

    @Test fun secondStoreOnSameFileSeesSavedValues() = runBlocking {
        val (first, firstJob) = newStore()
        first.setOnboardingComplete(true)
        first.setDefaultBudgetPaise(12_345_67L)
        // Only one DataStore may be active per file, so close the first before opening the second.
        firstJob.cancelAndJoin()

        val (second, _) = newStore()
        assertTrue(second.onboardingComplete.first())
        assertEquals(12_345_67L, second.defaultBudgetPaise.first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroBudgetThrows() = runBlocking {
        newStore().first.setDefaultBudgetPaise(0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeBudgetThrows() = runBlocking {
        newStore().first.setDefaultBudgetPaise(-5L)
    }

    @Test fun rejectedBudgetLeavesValueUnchanged() = runBlocking {
        val (store, _) = newStore()
        store.setDefaultBudgetPaise(100L)
        runCatching { store.setDefaultBudgetPaise(0L) }
        assertEquals(100L, store.defaultBudgetPaise.first())
    }
}
