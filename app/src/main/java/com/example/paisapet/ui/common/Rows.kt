package com.example.paisapet.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.paisapet.R
import com.example.paisapet.list.ChipKind
import com.example.paisapet.ui.home.RowModel
import com.example.paisapet.ui.theme.CardWhite
import com.example.paisapet.ui.theme.CategoryChip
import com.example.paisapet.ui.theme.InkSecondary
import com.example.paisapet.ui.theme.NotTaggedChip
import com.example.paisapet.ui.theme.OverBudgetRed
import com.example.paisapet.ui.theme.PeachIcon
import com.example.paisapet.ui.theme.PeachTile
import com.example.paisapet.ui.theme.SmsIcon
import com.example.paisapet.ui.theme.SmsTile
import com.example.paisapet.ui.theme.UntaggedRowTint

/**
 * One payment: icon, payee, subtitle, "–₹240" and the Misc / Essential / Not tagged chip.
 * Not tagged rows are tinted and their chip reads [untaggedChipText] ("Not tagged" on Home, "Tag me?" on S6).
 */
@Composable
fun PaymentRow(
    row: RowModel,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    untaggedChipText: String = "Not tagged",
) {
    val untagged = row.chip == ChipKind.NOT_TAGGED
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (untagged) UntaggedRowTint else CardWhite,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (tile, tint) = if (row.manual) PeachTile to PeachIcon else SmsTile to SmsIcon
            Box(
                modifier = Modifier.size(44.dp).background(tile, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (row.manual) R.drawable.ic_edit else R.drawable.ic_receipt),
                    contentDescription = if (row.manual) "Added manually" else "From SMS",
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.payee,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(row.amountText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.size(4.dp))
                CategoryChipLabel(row.chip, untaggedChipText)
            }
        }
    }
}

@Composable
fun CategoryChipLabel(kind: ChipKind, untaggedText: String = "Not tagged") {
    val (text, background, content) = when (kind) {
        ChipKind.MISC -> Triple("Misc", CategoryChip, InkSecondary)
        ChipKind.ESSENTIAL -> Triple("Essential", CategoryChip, InkSecondary)
        ChipKind.NOT_TAGGED -> Triple(untaggedText, NotTaggedChip, OverBudgetRed)
    }
    Chip(text, background, content)
}

@Composable
private fun Chip(text: String, background: Color, content: Color) {
    Surface(shape = CircleShape, color = background) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}
