package com.example.paisapet.widget

import android.content.Context
import com.example.paisapet.PaisaPetApp
import com.example.paisapet.budget.monthKeyOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

/**
 * The current month's widget model, updated by Room and DataStore. Emits null instead of throwing
 * when the data cannot be read, so the widget shows [WIDGET_ERROR_TEXT].
 * The month is fixed when collection starts; a Glance session lasts under a minute, and the next update reads again.
 */
internal fun widgetModels(context: Context): Flow<WidgetModel?> = flow<WidgetModel?> {
    val container = (context.applicationContext as PaisaPetApp).container
    val clock = container.clock
    val today = LocalDate.now(clock)
    val monthKey = monthKeyOf(clock.instant(), clock.zone)
    emitAll(
        combine(container.settings.onboardingComplete, container.repository.observeMonth(monthKey, today)) { done, view ->
            buildWidgetModel(done, view, monthKey)
        },
    )
}.catch { e ->
    if (e is CancellationException) throw e
    emit(null)
}
