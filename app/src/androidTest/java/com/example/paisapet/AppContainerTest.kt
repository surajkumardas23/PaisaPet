package com.example.paisapet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only: never inserts rows or changes settings in the real app's storage. */
@RunWith(AndroidJUnit4::class)
class AppContainerTest {

    private val app: Context = ApplicationProvider.getApplicationContext()

    private val container: AppContainer get() = (app as PaisaPetApp).container

    @Test fun applicationIsPaisaPetApp() = assertTrue(app is PaisaPetApp)

    @Test fun sameInstancesOnSecondAccess() {
        assertSame(container, container)
        assertSame(container.database, container.database)
        assertSame(container.settings, container.settings)
        assertSame(container.repository, container.repository)
    }

    @Test fun settingsReadWithoutError() = runBlocking<Unit> {
        container.settings.onboardingComplete.first()
        container.settings.defaultBudgetPaise.first()
    }

    @Test fun databaseOpens() = assertTrue(container.database.openHelper.writableDatabase.isOpen)
}
