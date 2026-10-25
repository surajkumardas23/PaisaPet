package com.example.paisapet.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.paisapet.R
import com.example.paisapet.model.Category
import com.example.paisapet.ui.theme.CardWhite
import com.example.paisapet.ui.theme.SurfaceContainer

/**
 * The Misc | Essential toggle (S7, S8). [selected] null shows neither chosen, as for a Not tagged payment.
 */
@Composable
fun CategoryToggle(
    selected: Category?,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(shape = CircleShape, color = SurfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for ((category, label) in listOf(Category.MISC to "Misc", Category.ESSENTIAL to "Essential")) {
                val isSelected = selected == category
                Surface(
                    onClick = { onSelect(category) },
                    enabled = enabled,
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else SurfaceContainer,
                    contentColor = if (isSelected) CardWhite else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .semantics {
                            role = Role.RadioButton
                            this.selected = isSelected
                        },
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(painterResource(R.drawable.ic_check_circle), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
