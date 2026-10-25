package com.example.paisapet.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.paisapet.R
import com.example.paisapet.debugtools.DebugTools
import com.example.paisapet.onboarding.notificationsRequired
import com.example.paisapet.ui.buddy.BuddyPlaceholder
import com.example.paisapet.ui.onboarding.isGranted
import com.example.paisapet.ui.onboarding.openAppSettings
import com.example.paisapet.ui.onboarding.openNotificationSettings
import com.example.paisapet.ui.theme.BuddyMint
import com.example.paisapet.ui.theme.CardWhite
import com.example.paisapet.ui.theme.HelperPill
import com.example.paisapet.ui.theme.InkSecondary
import com.example.paisapet.ui.theme.MintText
import com.example.paisapet.ui.theme.PeachIcon
import com.example.paisapet.ui.theme.PeachTile
import com.example.paisapet.ui.theme.SmsIcon
import com.example.paisapet.ui.theme.SmsTile
import com.example.paisapet.ui.theme.SurfaceContainer

/**
 * S9 (09-settings.png), shown inside the shell's pushed page. [budgetPaise] is the current month's budget,
 * null when none is set. Permission rows re-check every time the screen comes back to the front.
 */
@Composable
fun SettingsScreen(
    budgetPaise: Long?,
    onEditBudget: () -> Unit,
    onOpenDebug: () -> Unit,
) {
    val context = LocalContext.current
    val notificationsNeeded = notificationsRequired(Build.VERSION.SDK_INT)
    var smsGranted by remember { mutableStateOf(isGranted(context, Manifest.permission.RECEIVE_SMS)) }
    var notificationsGranted by remember { mutableStateOf(notificationsGranted(context)) }
    LifecycleResumeEffect(Unit) {
        smsGranted = isGranted(context, Manifest.permission.RECEIVE_SMS)
        notificationsGranted = notificationsGranted(context)
        onPauseOrDispose { }
    }
    val version = remember { versionText(appVersionName(context)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel("BUDGET")
        Card {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Monthly misc budget", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                budgetDisplayText(budgetPaise),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (budgetPaise != null) {
                                Spacer(Modifier.width(4.dp))
                                Text("/ month", style = MaterialTheme.typography.bodyMedium, color = InkSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                    }
                    IconButton(onClick = onEditBudget) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = "Edit budget", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, tint = SmsIcon, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Budget resets on the 1st of every month", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                }
            }
        }

        SectionLabel("DATA & PRIVACY")
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Step 10 wires the export up.
                SettingsRow(
                    icon = R.drawable.ic_download,
                    tile = SmsTile,
                    tint = SmsIcon,
                    title = "Export my data (CSV)",
                    subtitle = "Coming in the next step",
                    enabled = false,
                    onClick = {},
                )
                Text(
                    "If you uninstall the app, all data is deleted. Export first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                )
                Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp)) {
                        Icon(painterResource(R.drawable.ic_shield_check), contentDescription = null, tint = MintText, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("100% on-device privacy", style = MaterialTheme.typography.titleSmall, color = MintText)
                            Text("All data is stored only on this phone.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                        }
                    }
                }
            }
        }

        SectionLabel("APP PERMISSIONS")
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsRow(
                    icon = R.drawable.ic_sms,
                    tile = SmsTile,
                    tint = SmsIcon,
                    title = "SMS access",
                    subtitle = "Reads bank payment alerts",
                    status = smsStatusText(smsGranted),
                    onClick = { openAppSettings(context) },
                )
                HorizontalDivider(color = SurfaceContainer)
                SettingsRow(
                    icon = R.drawable.ic_bell,
                    tile = PeachTile,
                    tint = PeachIcon,
                    title = "Notifications",
                    subtitle = "Tag prompts",
                    status = notificationStatusText(notificationsNeeded, notificationsGranted),
                    onClick = { openNotificationSettings(context) },
                )
            }
        }

        if (DebugTools.isAvailable) {
            SectionLabel("DEBUG")
            Card {
                Column(Modifier.padding(16.dp)) {
                    SettingsRow(
                        icon = R.drawable.ic_bolt,
                        tile = SurfaceContainer,
                        tint = InkSecondary,
                        title = DebugTools.TITLE,
                        subtitle = null,
                        onClick = onOpenDebug,
                    )
                }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).background(BuddyMint, CircleShape), contentAlignment = Alignment.Center) {
                    BuddyPlaceholder(size = 44.dp)
                }
                Spacer(Modifier.height(10.dp))
                Text("Paisa Pet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(version, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                Spacer(Modifier.height(8.dp))
                Text("Built for Indian UPI spends · No ads", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 1.sp,
        color = InkSecondary,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun SettingsRow(
    @DrawableRes icon: Int,
    tile: Color,
    tint: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    status: String? = null,
    enabled: Boolean = true,
) {
    Surface(onClick = onClick, enabled = enabled, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(tile, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                val alpha = if (enabled) 1f else 0.5f
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = InkSecondary.copy(alpha = alpha)) }
            }
            if (status != null) {
                Surface(shape = CircleShape, color = HelperPill) {
                    Text(status, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
            } else {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = InkSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** POST_NOTIFICATIONS on Android 13+; below that there is nothing to grant. */
private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || isGranted(context, Manifest.permission.POST_NOTIFICATIONS)

/** versionName from the package (BuildConfig is off); null if it cannot be read. */
private fun appVersionName(context: Context): String? = try {
    val pm = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, 0)
    }
    info.versionName
} catch (e: PackageManager.NameNotFoundException) {
    null
}
