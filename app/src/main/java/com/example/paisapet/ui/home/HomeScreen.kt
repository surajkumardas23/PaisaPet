package com.example.paisapet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisapet.R
import com.example.paisapet.ui.buddy.BuddyPlaceholder
import com.example.paisapet.ui.buddy.buddyColor
import com.example.paisapet.ui.buddy.buddyTint
import com.example.paisapet.ui.common.MonthDropdown
import com.example.paisapet.ui.common.PastMonthBar
import com.example.paisapet.ui.common.PaymentRow
import com.example.paisapet.ui.common.ScreenTitle
import com.example.paisapet.ui.theme.CardWhite
import com.example.paisapet.ui.theme.InkSecondary
import com.example.paisapet.ui.theme.MintText
import com.example.paisapet.ui.theme.OverBudgetRed
import com.example.paisapet.ui.theme.SmsIcon
import com.example.paisapet.ui.theme.SmsTile
import com.example.paisapet.ui.theme.SurfaceContainer
import com.example.paisapet.ui.theme.SurfaceContainerHigh
import com.example.paisapet.ui.theme.UntaggedCard
import com.example.paisapet.ui.theme.WarningText

/** S4 and S5 (04-home-empty.png, 05-home.png). Draws [model] only; all numbers come from HomeModel. */
@Composable
fun HomeScreen(
    model: HomeModel,
    monthOptions: List<String>,
    currentKey: String,
    onSelectMonth: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onTagNow: () -> Unit,
    onSeeAll: () -> Unit,
    onRowClick: (id: Long) -> Unit,
    onSetBudget: () -> Unit,
    contentPadding: PaddingValues,
    /** Kept by the shell so the position survives switching tabs. */
    scrollState: ScrollState,
    /** FR-03: SMS permission is off; from showAutoTrackingBanner. */
    showAutoTrackingOff: Boolean = false,
    onOpenAppSettings: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Home")
        if (showAutoTrackingOff) AutoTrackingOffCard(onOpenAppSettings)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonthDropdown(model.monthKey, monthOptions, currentKey, onSelectMonth)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
            }
        }
        if (model.readOnly) PastMonthBar(model.backToCurrentLabel.orEmpty(), onBack = { onSelectMonth(currentKey) })

        when (val buddy = model.buddy) {
            BuddyCardModel.NoBudget -> NoBudgetCard(onSetBudget)
            is BuddyCardModel.Budgeted -> BuddyCard(buddy)
        }
        model.untagged?.let { UntaggedBanner(it, onTagNow) }
        if (model.showFreshMonthCard) FreshMonthCard()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (model.recent.isNotEmpty()) TextButton(onClick = onSeeAll) { Text("See all") }
        }
        if (model.emptyRecentText != null) {
            EmptyRecent(model.emptyRecentText)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in model.recent) PaymentRow(row, onClick = { onRowClick(row.id) })
            }
        }
        Spacer(Modifier.height(88.dp)) // room above the "+ Add expense" button
    }
}

@Composable
private fun AutoTrackingOffCard(onOpenAppSettings: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = WarningText, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Auto-tracking is off", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = WarningText)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Paisa Pet can't see your payment SMS. Allow SMS access to capture payments automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenAppSettings, shape = CircleShape) { Text("Open Settings") }
            }
        }
    }
}

@Composable
private fun BuddyCard(model: BuddyCardModel.Budgeted) {
    val color = buddyColor(model.state)
    val tint = buddyTint(model.state)
    Surface(shape = RoundedCornerShape(24.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.6f), CardWhite)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BuddyPlaceholder(size = 120.dp, state = model.state)
            Spacer(Modifier.height(8.dp))
            Text(model.homeLabel, style = MaterialTheme.typography.bodyMedium, color = color)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    model.amountText,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (model.overBudget) OverBudgetRed else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    model.amountCaption,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (model.overBudget) OverBudgetRed else InkSecondary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Surface(shape = CircleShape, color = tint) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).background(color, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(model.chipText, style = MaterialTheme.typography.labelLarge, color = color)
                }
            }
            Spacer(Modifier.height(16.dp))
            ProgressBar(model.barPercent, color)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(model.footerStart, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                Spacer(Modifier.weight(1f))
                model.footerEnd?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = InkSecondary) }
            }
        }
    }
}

@Composable
private fun ProgressBar(percent: Int, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(SurfaceContainer)
            .semantics { contentDescription = "$percent% of budget used" },
    ) {
        if (percent > 0) {
            Box(
                Modifier
                    .fillMaxWidth(percent / 100f)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun NoBudgetCard(onSetBudget: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BuddyPlaceholder(size = 96.dp)
            Spacer(Modifier.height(12.dp))
            Text("No budget set", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Set a monthly misc budget so Buddy can track it.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onSetBudget) { Text("Set budget") }
        }
    }
}

@Composable
private fun UntaggedBanner(model: UntaggedModel, onTagNow: () -> Unit) {
    if (!model.tagNow) {
        Text(model.title, style = MaterialTheme.typography.bodyMedium, color = WarningText)
        return
    }
    Surface(shape = RoundedCornerShape(20.dp), color = UntaggedCard, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(model.title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                model.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f)) }
            }
            Spacer(Modifier.width(8.dp))
            Surface(onClick = onTagNow, shape = CircleShape, color = Color.White) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tag now", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(4.dp))
                    Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FreshMonthCard() {
    Surface(shape = RoundedCornerShape(24.dp), color = SurfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp)) {
            Box(Modifier.size(44.dp).background(SmsTile, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_bolt), contentDescription = null, tint = SmsIcon, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Fresh month, clean slate!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your payments will appear here automatically when bank debit SMS arrive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TipLine(R.drawable.ic_check_circle, "No manual math needed")
                        TipLine(R.drawable.ic_bolt, "Just tap Misc or Essential in notification popups")
                    }
                }
            }
        }
    }
}

@Composable
private fun TipLine(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = MintText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyRecent(text: String) {
    Surface(shape = RoundedCornerShape(24.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).background(SurfaceContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_receipt), contentDescription = null, tint = InkSecondary)
            }
            Spacer(Modifier.height(12.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
