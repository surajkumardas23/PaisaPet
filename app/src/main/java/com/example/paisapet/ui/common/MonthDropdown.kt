package com.example.paisapet.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.paisapet.R
import com.example.paisapet.list.monthLabel
import com.example.paisapet.ui.theme.HelperPill

/**
 * The month pill "October 2026 ▾" and its menu (FR-23). [options] comes from monthOptions:
 * the current month first, then the months with data, newest first.
 */
@Composable
fun MonthDropdown(
    selected: String,
    options: List<String>,
    currentKey: String,
    onSelect: (monthKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { open = true },
            shape = CircleShape,
            color = HelperPill,
            modifier = Modifier.semantics { contentDescription = "Month: ${monthLabel(selected)}. Change month" },
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(monthLabel(selected), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (key in options) {
                val label = if (key == currentKey) "${monthLabel(key)} · this month" else monthLabel(key)
                DropdownMenuItem(
                    text = {
                        Text(label, fontWeight = if (key == selected) FontWeight.Bold else FontWeight.Normal)
                    },
                    onClick = {
                        open = false
                        onSelect(key)
                    },
                )
            }
        }
    }
}
