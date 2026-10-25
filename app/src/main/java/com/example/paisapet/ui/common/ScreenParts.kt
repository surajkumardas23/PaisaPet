package com.example.paisapet.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.paisapet.ui.theme.HelperPill
import com.example.paisapet.ui.theme.InkSecondary
import com.example.paisapet.ui.theme.SmsIcon
import com.example.paisapet.ui.theme.SmsTile

/** "Paisa Pet" over the screen name, as at the top of Home and Transactions. */
@Composable
fun ScreenTitle(title: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text("Paisa Pet", style = MaterialTheme.typography.labelLarge, color = InkSecondary)
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** "Past month · view only" and the "Back to October 2026" chip. */
@Composable
fun PastMonthBar(backLabel: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = CircleShape, color = HelperPill) {
            Text(
                "Past month · view only",
                style = MaterialTheme.typography.labelLarge,
                color = InkSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Surface(onClick = onBack, shape = CircleShape, color = SmsTile) {
            Text(
                backLabel,
                style = MaterialTheme.typography.labelLarge,
                color = SmsIcon,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
