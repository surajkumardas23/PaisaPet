package com.example.paisapet.ui.onboarding

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.paisapet.R
import com.example.paisapet.onboarding.PermissionCard
import com.example.paisapet.onboarding.notificationsRequired
import com.example.paisapet.onboarding.permissionCard
import com.example.paisapet.onboarding.showRestrictedHint
import com.example.paisapet.onboarding.showSmsDeniedCard
import com.example.paisapet.ui.buddy.BuddyPlaceholder
import com.example.paisapet.ui.theme.AllowedChipBackground
import com.example.paisapet.ui.theme.AllowedChipText
import com.example.paisapet.ui.theme.BuddyMint
import com.example.paisapet.ui.theme.CardWhite
import com.example.paisapet.ui.theme.HelperPill
import com.example.paisapet.ui.theme.MintText
import com.example.paisapet.ui.theme.PeachIcon
import com.example.paisapet.ui.theme.PeachTile
import com.example.paisapet.ui.theme.SmsIcon
import com.example.paisapet.ui.theme.SmsTile
import com.example.paisapet.ui.theme.SurfaceContainer
import com.example.paisapet.ui.theme.SurfaceContainerHigh
import com.example.paisapet.ui.theme.WarningText

/** What Android says right now about the two permissions. */
private data class PermissionFacts(
    val smsGranted: Boolean,
    val smsRationale: Boolean,
    val notificationsGranted: Boolean,
    val notificationsRationale: Boolean,
)

private fun readFacts(context: Context): PermissionFacts {
    val activity = context.findActivity()
    val sms = Manifest.permission.RECEIVE_SMS
    val notificationsAsked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    return PermissionFacts(
        smsGranted = isGranted(context, sms),
        smsRationale = activity != null && shouldShowRationale(activity, sms),
        notificationsGranted = if (notificationsAsked) {
            isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        },
        notificationsRationale = notificationsAsked && activity != null &&
            shouldShowRationale(activity, Manifest.permission.POST_NOTIFICATIONS),
    )
}

/**
 * S3 Permissions (03-permissions.png). [onFinish] marks onboarding complete ("Go to Home" and "Continue manual");
 * AppRoot then shows Home by itself.
 */
@Composable
fun PermissionsScreen(finishFailed: Boolean, onBack: () -> Unit, onFinish: () -> Unit) {
    val context = LocalContext.current
    var facts by remember { mutableStateOf(readFacts(context)) }
    var smsDeniedThisVisit by rememberSaveable { mutableStateOf(false) }
    var notificationsDeniedThisVisit by rememberSaveable { mutableStateOf(false) }

    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) smsDeniedThisVisit = true
        facts = readFacts(context)
    }
    val notificationsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) notificationsDeniedThisVisit = true
            facts = readFacts(context)
        }
    // Coming back from system settings (or the dialog) re-checks both permissions.
    LifecycleResumeEffect(Unit) {
        facts = readFacts(context)
        onPauseOrDispose { }
    }

    val sdk = Build.VERSION.SDK_INT
    val smsCard = permissionCard(
        required = true,
        granted = facts.smsGranted,
        showRationale = facts.smsRationale,
        deniedThisVisit = smsDeniedThisVisit,
    )
    val notificationsCard = permissionCard(
        required = notificationsRequired(sdk),
        granted = facts.notificationsGranted,
        showRationale = facts.notificationsRationale,
        deniedThisVisit = notificationsDeniedThisVisit,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).background(HelperPill, CircleShape),
            ) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
            }
            StepIndicator(current = 2, total = 2, modifier = Modifier.align(Alignment.Center))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header()
            SmsCard(
                card = smsCard,
                onAllow = { smsLauncher.launch(Manifest.permission.RECEIVE_SMS) },
                onOpenSettings = { openAppSettings(context) },
            )
            NotificationsCard(
                card = notificationsCard,
                onAllow = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onOpenSettings = { openNotificationSettings(context) },
            )
            PrivacyCard()
            if (showSmsDeniedCard(smsCard)) {
                SmsDeniedCard(
                    showHint = showRestrictedHint(smsCard, sdk),
                    onOpenSettings = { openAppSettings(context) },
                    onContinueManual = onFinish,
                )
            }
        }
        if (finishFailed) {
            Text(
                "Couldn't save. Try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        PrimaryPillButton(
            text = "Go to Home",
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Two quick permissions to automate your tracking",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Buddy detects small UPI spends in real-time so nothing slips past your radar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box {
            Box(
                modifier = Modifier.size(88.dp).background(BuddyMint.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BuddyPlaceholder(size = 56.dp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_shield_check),
                    contentDescription = null,
                    tint = CardWhite,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SmsCard(card: PermissionCard, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    InfoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            CardHeader(R.drawable.ic_sms, SmsTile, SmsIcon, "SMS Access", card)
            Text(
                "To catch your payments automatically. We only read bank debit messages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 58.dp),
            )
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_shield_check),
                        contentDescription = null,
                        tint = MintText,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Only UPI debit messages are used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CardAction(card, onAllow, onOpenSettings, leading = {})
        }
    }
}

@Composable
private fun NotificationsCard(card: PermissionCard, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    InfoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            CardHeader(R.drawable.ic_bell, PeachTile, PeachIcon, "Notifications", card)
            Text(
                buildAnnotatedString {
                    append("To ask ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                        append("Misc")
                    }
                    append(" or ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                        append("Essential")
                    }
                    append(" right after you pay.")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 58.dp),
            )
            CardAction(card, onAllow, onOpenSettings) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_bolt),
                        contentDescription = null,
                        tint = PeachIcon,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Single-tap classification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Tile, title and (for Allowed, Not needed, Not allowed) the status chip on the right. */
@Composable
private fun CardHeader(@DrawableRes icon: Int, tileColor: Color, iconColor: Color, title: String, card: PermissionCard) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        IconTile(icon = icon, tileColor = tileColor, iconColor = iconColor)
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        when (card) {
            PermissionCard.ALLOWED -> StatusChip("Allowed", AllowedChipBackground, AllowedChipText, R.drawable.ic_check_circle)
            PermissionCard.NOT_NEEDED -> StatusChip("Not needed", HelperPill, MaterialTheme.colorScheme.onSurfaceVariant)
            PermissionCard.DENIED_CAN_ASK, PermissionCard.BLOCKED -> StatusChip("Not allowed", PeachTile, WarningText)
            PermissionCard.ASK -> Unit
        }
    }
}

/** The bottom row: [leading] on the left, Allow or Open Settings on the right when the card needs one. */
@Composable
private fun CardAction(
    card: PermissionCard,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    leading: @Composable () -> Unit,
) {
    val action: (@Composable () -> Unit)? = when (card) {
        PermissionCard.ASK, PermissionCard.DENIED_CAN_ASK -> {
            { SmallPillButton("Allow", R.drawable.ic_touch, filled = true, onClick = onAllow) }
        }
        PermissionCard.BLOCKED -> {
            { SmallPillButton("Open Settings", R.drawable.ic_settings, filled = true, onClick = onOpenSettings) }
        }
        PermissionCard.ALLOWED, PermissionCard.NOT_NEEDED -> null
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) { leading() }
        action?.invoke()
    }
}

@Composable
private fun StatusChip(text: String, background: Color, content: Color, @DrawableRes icon: Int? = null) {
    Surface(shape = CircleShape, color = background, contentColor = content) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A 48dp pill: blue (Allow) or white (Open Settings on the denied card). */
@Composable
private fun SmallPillButton(text: String, @DrawableRes icon: Int, filled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = if (filled) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.buttonColors(containerColor = CardWhite, contentColor = MaterialTheme.colorScheme.onSurface)
        },
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        if (!filled) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (filled) {
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PrivacyCard() {
    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(CardWhite, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = SmsIcon,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Nothing leaves your phone • Zero cloud sync • Completely private",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SmsDeniedCard(showHint: Boolean, onOpenSettings: () -> Unit, onContinueManual: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                IconTile(icon = R.drawable.ic_warning, tileColor = PeachTile, iconColor = WarningText)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "SMS Permission Denied",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Without SMS access, auto-tracking won't work, but you can still add expenses manually.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (showHint) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "If the Allow button is greyed out in Settings, tap the three dots and choose " +
                                "Allow restricted settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningText,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallPillButton("Open Settings", R.drawable.ic_settings, filled = false, onClick = onOpenSettings)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onContinueManual, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Continue manual", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
