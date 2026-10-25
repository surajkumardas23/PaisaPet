package com.example.paisapet.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.paisapet.list.TxFilter
import com.example.paisapet.ui.common.MonthDropdown
import com.example.paisapet.ui.common.PastMonthBar
import com.example.paisapet.ui.common.PaymentRow
import com.example.paisapet.ui.common.ScreenTitle
import com.example.paisapet.ui.theme.CardWhite
import com.example.paisapet.ui.theme.CategoryChip
import com.example.paisapet.ui.theme.InkSecondary
import com.example.paisapet.ui.theme.SmsTile
import com.example.paisapet.ui.theme.SurfaceContainer
import com.example.paisapet.ui.theme.SurfaceContainerHigh

/** S6 (06-transactions.png). Draws [model] only; all numbers come from TransactionsModel. */
@Composable
fun TransactionsScreen(
    model: TransactionsModel,
    monthOptions: List<String>,
    currentKey: String,
    onSelectMonth: (String) -> Unit,
    onSelectFilter: (TxFilter) -> Unit,
    onRowClick: (id: Long) -> Unit,
    contentPadding: PaddingValues,
    /** Kept by the shell so the position survives switching tabs. */
    listState: LazyListState,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp, // room above "+ Add expense"
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "title") { ScreenTitle("Transactions") }
        item(key = "month") { MonthDropdown(model.monthKey, monthOptions, currentKey, onSelectMonth) }
        if (model.readOnly) {
            item(key = "past") { PastMonthBar(model.backToCurrentLabel.orEmpty(), onBack = { onSelectMonth(currentKey) }) }
        }
        item(key = "header") { HeaderCard(model.header) }
        item(key = "chips") { FilterChips(model.chips, onSelectFilter) }
        if (model.emptyText != null) {
            item(key = "empty") {
                Text(
                    model.emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSecondary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                )
            }
        }
        for (group in model.groups) {
            item(key = "day-${group.label}") {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
                    Text(group.label, style = MaterialTheme.typography.labelLarge, color = InkSecondary)
                    Spacer(Modifier.weight(1f))
                    Text(group.totalText, style = MaterialTheme.typography.labelLarge, color = InkSecondary)
                }
            }
            items(group.rows, key = { it.id }) { row ->
                PaymentRow(row, onClick = { onRowClick(row.id) }, untaggedChipText = model.untaggedChipText)
            }
        }
    }
}

@Composable
private fun HeaderCard(h: TxHeaderModel) {
    Surface(shape = RoundedCornerShape(24.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("TOTAL DEBITS", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(h.totalText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(h.countText, style = MaterialTheme.typography.bodyMedium, color = InkSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SplitTile("Misc spends", h.miscText, h.miscPctText, SmsTile, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                SplitTile("Essential", h.essentialText, h.essentialPctText, SurfaceContainer, InkSecondary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            SplitBar(h.miscPercent)
        }
    }
}

@Composable
private fun SplitTile(title: String, amount: String, pct: String, background: Color, accent: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = background, modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = accent, modifier = Modifier.weight(1f))
                Box(Modifier.size(8.dp).background(accent, CircleShape))
            }
            Text(amount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(pct, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        }
    }
}

@Composable
private fun SplitBar(miscPercent: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(SurfaceContainerHigh)
            .semantics { contentDescription = "Misc is $miscPercent% of total debits" },
    ) {
        if (miscPercent > 0) {
            Box(
                Modifier
                    .fillMaxWidth(miscPercent / 100f)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun FilterChips(chips: List<FilterChipModel>, onSelect: (TxFilter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (chip in chips) {
            Surface(
                onClick = { onSelect(chip.filter) },
                shape = CircleShape,
                color = if (chip.selected) MaterialTheme.colorScheme.primary else CategoryChip,
                contentColor = if (chip.selected) CardWhite else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics {
                    role = Role.Tab
                    selected = chip.selected
                },
            ) {
                Text(
                    chip.label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}
