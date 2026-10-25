package com.example.paisapet.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.example.paisapet.R
import com.example.paisapet.ui.theme.CardWhite
import com.example.paisapet.ui.theme.StepPill

/** The white rounded card used on all three onboarding screens. */
@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = CardWhite, content = content)
}

/** The big blue pill button with a forward arrow: Get started, Continue, Go to Home. */
@Composable
fun PrimaryPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier.height(56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

/** "Step 1 of 2 ●○": the stepper pill at the top of S2 and S3. */
@Composable
fun StepIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = StepPill) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Step $current of $total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            for (i in 1..total) {
                val alpha = if (i <= current) 1f else 0.25f
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), CircleShape),
                )
            }
        }
    }
}

/** A tinted rounded square with an icon, as on the feature and permission cards. */
@Composable
fun IconTile(@DrawableRes icon: Int, tileColor: Color, iconColor: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(12.dp), color = tileColor, modifier = Modifier.size(44.dp)) {}
        Icon(painterResource(icon), contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
    }
}
