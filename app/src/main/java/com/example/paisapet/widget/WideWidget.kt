package com.example.paisapet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.paisapet.R
import com.example.paisapet.nav.TARGET_ADD
import com.example.paisapet.nav.TARGET_HOME
import com.example.paisapet.ui.buddy.buddyColor
import com.example.paisapet.ui.buddy.buddyTint
import com.example.paisapet.ui.theme.CardWhite
import com.example.paisapet.ui.theme.InkText
import com.example.paisapet.ui.theme.OverBudgetRed
import kotlinx.coroutines.flow.first

/**
 * The 4x2 widget (S11): Buddy and the % chip on the left; the amount, "+ Add", budget line, bar and last payment on the right.
 * A tap opens Home; "+ Add" opens the Add sheet. Parts that do not fit after a resize are dropped (see [wideFit]).
 */
class WideWidget : GlanceAppWidget() {

    // Exact: the bar's fill is a width in dp, so the widget needs its real size after a resize.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val models = widgetModels(context)
        // The first frame from a one-shot read; then the same flow, so an update() during a live session shows fresh data.
        val first = models.first()
        provideContent {
            val model by models.collectAsState(initial = first)
            WideContent(model)
        }
    }
}

class WideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WideWidget()
}

@Composable
private fun WideContent(model: WidgetModel?) {
    val context = LocalContext.current
    WidgetCard(onTap = openAppIntent(context, TARGET_HOME)) {
        if (model !is WidgetModel.Budgeted) {
            // SetUp, NoBudget and the fallback: no "+ Add" (Add needs onboarding done and a budget).
            MessageContent(messageFor(model), buddySize = 56.dp)
        } else {
            val size = LocalSize.current
            val fit = wideFit(size.width.value, size.height.value, WidgetPadding.value)
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (fit.showBuddy) {
                    Column(
                        GlanceModifier.width(WIDE_LEFT_COLUMN_DP.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BuddyImage(model.state, 56.dp)
                        Spacer(GlanceModifier.height(6.dp))
                        PctChip(model)
                    }
                    Spacer(GlanceModifier.width(WIDE_COLUMN_GAP_DP.dp))
                }
                NumbersColumn(model, fit, context)
            }
        }
    }
}

@Composable
private fun NumbersColumn(m: WidgetModel.Budgeted, fit: WideFit, context: Context) {
    Column(GlanceModifier.fillMaxWidth()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(GlanceModifier.defaultWeight()) {
                AmountText(m, 26.sp, align = TextAlign.Start)
            }
            AddButton(context)
        }
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SmallText(m.ofBudgetText, color = if (m.overBudget) OverBudgetRed else InkText, align = TextAlign.Start)
            if (!fit.showBuddy) {
                Spacer(GlanceModifier.width(6.dp))
                PctChip(m)
            }
        }
        if (fit.showBudgetLine) {
            SmallText(m.budgetLine, align = TextAlign.Start)
        }
        Spacer(GlanceModifier.height(6.dp))
        BudgetBar(m.barPercent, buddyColor(m.state), fit.barTrackDp.dp)
        if (fit.showLastPayment) {
            Spacer(GlanceModifier.height(6.dp))
            SmallText(m.lastPayment ?: WIDGET_NO_PAYMENTS_TEXT, align = TextAlign.Start)
        }
    }
}

/** "64% used" on Buddy's tint. Corners are round on Android 12+, square before. */
@Composable
private fun PctChip(m: WidgetModel.Budgeted) {
    Box(
        GlanceModifier
            .background(buddyTint(m.state))
            .cornerRadius(12.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        SmallText(m.pctChip, color = InkText, bold = true)
    }
}

/** Its own intent (data paisapet://open/add), so it never shares a PendingIntent with the card's Home tap. */
@Composable
private fun AddButton(context: Context) {
    Box(
        GlanceModifier
            .background(ImageProvider(R.drawable.widget_add_pill))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(actionStartActivity(openAppIntent(context, TARGET_ADD))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = context.getString(R.string.widget_add),
            style = TextStyle(color = ColorProvider(CardWhite), fontSize = 13.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}
