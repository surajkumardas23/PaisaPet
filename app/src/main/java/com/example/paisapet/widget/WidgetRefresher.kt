package com.example.paisapet.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException

/** Redraws the widgets from fresh data. Does nothing, and starts no work, when no widget is placed. */
class WidgetRefresher(context: Context) {

    private val context = context.applicationContext

    /** Never throws, except [CancellationException]. One widget failing does not stop the other. */
    suspend fun refreshIfPlaced() {
        refresh(SmallWidget::class.java) { SmallWidget() }
        refresh(WideWidget::class.java) { WideWidget() }
    }

    /** Updates [kind] only if at least one of it is placed. */
    private suspend fun <T : GlanceAppWidget> refresh(kind: Class<T>, create: () -> T) {
        try {
            if (GlanceAppWidgetManager(context).getGlanceIds(kind).isNotEmpty()) {
                create().updateAll(context)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A failed redraw is harmless: the widget catches up on the next refresh.
        }
    }
}
