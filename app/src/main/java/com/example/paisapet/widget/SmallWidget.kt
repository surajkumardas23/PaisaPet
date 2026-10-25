package com.example.paisapet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.TextAlign
import com.example.paisapet.R
import com.example.paisapet.nav.TARGET_HOME
import com.example.paisapet.ui.buddy.buddyColor
import kotlinx.coroutines.flow.first

/** The 2x2 widget (S11): "Paisa Pet", Buddy, ₹ left of ₹ budget, and the bar. A tap opens Home. */
class SmallWidget : GlanceAppWidget() {

    // Exact: the bar's fill is a width in dp, so the widget needs its real size after a resize.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val models = widgetModels(context)
        // The first frame from a one-shot read; then the same flow, so an update() during a live session shows fresh data.
        val first = models.first()
        provideContent {
            val model by models.collectAsState(initial = first)
            SmallContent(model)
        }
    }
}

class SmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallWidget()
}

@Composable
private fun SmallContent(model: WidgetModel?) {
    val context = LocalContext.current
    WidgetCard(onTap = openAppIntent(context, TARGET_HOME)) {
        if (model !is WidgetModel.Budgeted) {
            MessageContent(messageFor(model), buddySize = 48.dp)
        } else {
            Column(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    SmallText(context.getString(R.string.widget_small_name), bold = true, align = TextAlign.Start)
                }
                BuddyImage(model.state, 48.dp)
                AmountText(model, 24.sp)
                SmallText(model.ofBudgetText)
                Spacer(GlanceModifier.height(8.dp))
                BudgetBar(model.barPercent, buddyColor(model.state), LocalSize.current.width - WidgetPadding * 2)
            }
        }
    }
}
