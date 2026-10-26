package com.example.paisapet.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.paisapet.MainActivity
import com.example.paisapet.R
import com.example.paisapet.budget.BuddyState
import com.example.paisapet.nav.EXTRA_OPEN_TARGET
import com.example.paisapet.ui.buddy.buddyDrawable
import com.example.paisapet.ui.buddy.buddyName
import com.example.paisapet.ui.theme.InkSecondary
import com.example.paisapet.ui.theme.InkText
import com.example.paisapet.ui.theme.OverBudgetRed
import com.example.paisapet.ui.theme.SurfaceContainer

// Shared Glance pieces for the widgets. Everything shown comes from WidgetModel; no budget maths here.

val WidgetPadding = 12.dp
private val BarHeight = 8.dp

/** Opens MainActivity with EXTRA_OPEN_TARGET [target] ("home" or "add"); the data URI keeps each target's intent distinct. */
fun openAppIntent(context: Context, target: String): Intent =
    Intent(context, MainActivity::class.java)
        .setData("paisapet://open/$target".toUri())
        .putExtra(EXTRA_OPEN_TARGET, target)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

/** The rounded white card every widget sits on; a tap anywhere runs [onTap]. */
@Composable
fun WidgetCard(onTap: Intent, content: @Composable () -> Unit) {
    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_background))
            .cornerRadius(24.dp)
            .padding(WidgetPadding)
            .clickable(actionStartActivity(onTap)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun BuddyImage(state: BuddyState, size: Dp) {
    Image(
        provider = ImageProvider(buddyDrawable(state)),
        contentDescription = "Buddy, ${buddyName(state)}",
        modifier = GlanceModifier.size(size),
    )
}

/** A rounded bar [trackWidth] wide, filled to [percent] in [color]. Made of Boxes so the colour works on every Android version. */
@Composable
fun BudgetBar(percent: Int, color: Color, trackWidth: Dp) {
    val fill = barFillDp(trackWidth.value, percent)
    Box(
        GlanceModifier
            .fillMaxWidth()
            .height(BarHeight)
            .background(SurfaceContainer)
            .cornerRadius(BarHeight / 2),
    ) {
        if (fill > 0f) {
            Box(
                GlanceModifier
                    .width(fill.dp)
                    .height(BarHeight)
                    .background(color)
                    .cornerRadius(BarHeight / 2),
            ) {}
        }
    }
}

/** The amount, big: red when over budget. */
@Composable
fun AmountText(m: WidgetModel.Budgeted, fontSize: TextUnit, align: TextAlign = TextAlign.Center) {
    Text(
        text = m.amountText,
        style = TextStyle(
            color = ColorProvider(if (m.overBudget) OverBudgetRed else InkText),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = align,
        ),
        maxLines = 1,
    )
}

@Composable
fun SmallText(text: String, color: Color = InkSecondary, bold: Boolean = false, align: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        style = TextStyle(
            color = ColorProvider(color),
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = align,
        ),
        maxLines = 1,
    )
}

/** SetUp, NoBudget and the read-failure fallback: thriving Buddy and one line. */
@Composable
fun MessageContent(message: String, buddySize: Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
        BuddyImage(BuddyState.THRIVING, buddySize)
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = message,
            style = TextStyle(
                color = ColorProvider(InkText),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/** The one line shown instead of numbers: set up, no budget, or the read-failure fallback (null). */
fun messageFor(model: WidgetModel?): String = when (model) {
    WidgetModel.SetUp -> WIDGET_SET_UP_TEXT
    WidgetModel.NoBudget -> WIDGET_NO_BUDGET_TEXT
    null, is WidgetModel.Budgeted -> WIDGET_ERROR_TEXT
}
