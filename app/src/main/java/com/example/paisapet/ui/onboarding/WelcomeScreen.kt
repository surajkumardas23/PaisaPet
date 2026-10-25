package com.example.paisapet.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisapet.R
import com.example.paisapet.ui.buddy.BuddyPlaceholder
import com.example.paisapet.ui.theme.BuddyMint
import com.example.paisapet.ui.theme.HelperPill
import com.example.paisapet.ui.theme.HeroLavender
import com.example.paisapet.ui.theme.HeroMint
import com.example.paisapet.ui.theme.MintIcon
import com.example.paisapet.ui.theme.MintText
import com.example.paisapet.ui.theme.PeachIcon
import com.example.paisapet.ui.theme.PeachTile
import com.example.paisapet.ui.theme.SmsIcon
import com.example.paisapet.ui.theme.SmsTile

/** S1 Welcome (01-welcome.png, with the PRD changes: Paisa Pet, UPI-only copy, no stepper). */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OnDevicePill()
            HeroCard()
            FeatureCard(
                icon = R.drawable.ic_sms,
                tileColor = SmsTile,
                iconColor = SmsIcon,
                title = "Reads your payment SMS",
                body = "Catches UPI debit alerts instantly",
            )
            FeatureCard(
                icon = R.drawable.ic_touch,
                tileColor = BuddyMint,
                iconColor = MintIcon,
                title = "One tap to tag",
                body = "Just answer “Misc or Essential?” for each payment",
            )
            FeatureCard(
                icon = R.drawable.ic_phone_lock,
                tileColor = PeachTile,
                iconColor = PeachIcon,
                title = "Your data stays on your phone",
                body = "100% offline, zero cloud servers & secure",
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_shield_check),
                contentDescription = null,
                tint = MintText,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "No login or sign-up needed • Works offline",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryPillButton(
            text = "Get started",
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun OnDevicePill() {
    Surface(shape = CircleShape, color = HelperPill) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(MintText.copy(alpha = 0.55f), CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                "100% On-device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroCard() {
    InfoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .drawBehind {
                    // Soft mint glow top-right and a faint lavender wash bottom-left, as in the mockup.
                    drawRect(
                        Brush.radialGradient(
                            listOf(HeroMint, Color.Transparent),
                            center = Offset(size.width, 0f),
                            radius = size.width * 0.6f,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(HeroLavender, Color.Transparent),
                            center = Offset(0f, size.height),
                            radius = size.width * 0.6f,
                        ),
                    )
                }
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BuddyPlaceholder(size = 104.dp)
            Spacer(Modifier.height(20.dp))
            Surface(shape = CircleShape, color = BuddyMint) {
                Text(
                    "“Hey! I'm Buddy, your spend pal”",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MintText,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Paisa Pet",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Know your untracked misc spending before the month ends.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FeatureCard(@DrawableRes icon: Int, tileColor: Color, iconColor: Color, title: String, body: String) {
    InfoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = icon, tileColor = tileColor, iconColor = iconColor)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
