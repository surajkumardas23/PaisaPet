package com.example.paisapet.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException

/** Redraws the widgets from fresh data. Does nothing, and starts no work, when no widget is placed. */
class WidgetRefresher(context: Context) {

    private val context = context.applicationContext

    /** Never throws, except [CancellationException]. */
    suspend fun refreshIfPlaced() {
        try {
            if (GlanceAppWidgetManager(context).getGlanceIds(SmallWidget::class.java).isNotEmpty()) {
                SmallWidget().updateAll(context)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A failed redraw is harmless: the widget catches up on the next refresh.
        }
    }
}
