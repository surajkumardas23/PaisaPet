package com.example.paisapet.debugtools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.paisapet.AppContainer
import com.example.paisapet.budget.monthKeyOf
import com.example.paisapet.format.formatRupees
import com.example.paisapet.sms.IncomingSms
import com.example.paisapet.sms.PipelineResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

private val TRACE_TIME = DateTimeFormatter.ofPattern("dd MMM HH:mm:ss", Locale.ENGLISH)

/** Debug-only Simulate SMS (FR-19): feeds the same pipeline as the SMS receiver. Logs nothing. */
@Composable
internal fun SimulateSmsScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sender by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf("") }
    var smsGranted by remember { mutableStateOf(isGranted(context, Manifest.permission.RECEIVE_SMS)) }
    var notificationStatus by remember { mutableStateOf(notificationStatus(context)) }
    var trace by remember { mutableStateOf(ReceiverTrace.read(context)) }

    fun refresh() {
        smsGranted = isGranted(context, Manifest.permission.RECEIVE_SMS)
        notificationStatus = notificationStatus(context)
        trace = ReceiverTrace.read(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }

    fun fill(bank: SampleBank) {
        val sample = generate(bank, Random.Default, LocalDate.now())
        sender = sample.sender
        body = sample.body
        outcome = ""
    }

    val zone = remember { ZoneId.systemDefault() }
    val monthFlow = remember {
        container.repository.observeMonth(monthKeyOf(Instant.now(), zone), LocalDate.now(zone))
    }
    val month by monthFlow.collectAsState(initial = null)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Simulate SMS (debug only)", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = sender,
            onValueChange = { sender = it },
            label = { Text("Sender") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Body") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { fill(SampleBank.HDFC) }) { Text("HDFC sample") }
            OutlinedButton(onClick = { fill(SampleBank.HDFC_AUTOPAY) }) { Text("HDFC AutoPay sample") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { fill(SampleBank.SBI) }) { Text("SBI sample") }
            OutlinedButton(onClick = { fill(SampleBank.ICICI) }) { Text("ICICI sample") }
        }
        Button(
            onClick = {
                val sms = IncomingSms(sender, body, Instant.now())
                scope.launch {
                    val result = withContext(Dispatchers.IO) { container.smsPipeline.process(sms) }
                    outcome = describe(result)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Run") }
        if (outcome.isNotEmpty()) Text("Outcome: $outcome", style = MaterialTheme.typography.titleMedium)

        HorizontalDivider()
        Text("Permissions", style = MaterialTheme.typography.titleMedium)
        Text("SMS: ${if (smsGranted) "granted" else "NOT granted"}")
        Text("Notifications: $notificationStatus")
        OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) }) {
            Text("Request SMS permission")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text("Request notification permission")
            }
        } else {
            Text("Notification permission is not needed below Android 13.")
        }
        OutlinedButton(onClick = { context.startActivity(appSettingsIntent(context)) }) { Text("Open app settings") }

        HorizontalDivider()
        Text("Budget", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { scope.launch { container.settings.setDefaultBudgetPaise(500_000) } }) {
            Text("Set default budget to ₹5,000")
        }
        val summary = month?.summary
        if (summary == null) {
            Text("No budget set.")
        } else {
            Text("Month ${summary.monthKey}: budget ${formatRupees(summary.budgetPaise)}")
            Text("Left ${formatRupees(summary.leftPaise)} · ${summary.pctUsedFloor}% used · Buddy ${summary.buddyState}")
        }
        Text("This month's payments", style = MaterialTheme.typography.titleMedium)
        val rows = month?.transactions.orEmpty()
        if (rows.isEmpty()) Text("None yet.")
        rows.forEach { Text("${it.payee}, ${formatRupees(it.amountPaise)}, ${it.category}") }

        HorizontalDivider()
        Text("Last real SMS handled by the receiver", style = MaterialTheme.typography.titleMedium)
        val last = trace
        if (last == null) {
            Text("None yet.")
        } else {
            val time = Instant.ofEpochMilli(last.timeMillis).atZone(zone).format(TRACE_TIME)
            Text(listOf(last.kind, last.detail, time).filter { it.isNotEmpty() }.joinToString(" · "))
        }
        OutlinedButton(onClick = { trace = ReceiverTrace.read(context) }) { Text("Refresh") }
    }
}

private fun describe(result: PipelineResult): String = when (result) {
    is PipelineResult.Saved -> "Saved (id ${result.id}), " + if (result.prompted) "prompt shown" else "prompt NOT shown"
    PipelineResult.Duplicate -> "Duplicate"
    is PipelineResult.Ignored -> "Ignored: ${result.reason.code}"
    is PipelineResult.Failed -> "Failed at ${result.stage}"
}

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun notificationStatus(context: Context): String {
    val enabled = if (NotificationManagerCompat.from(context).areNotificationsEnabled()) "on" else "OFF"
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = if (isGranted(context, Manifest.permission.POST_NOTIFICATIONS)) "granted" else "NOT granted"
        "$granted (app notifications $enabled)"
    } else {
        "not needed (app notifications $enabled)"
    }
}

private fun appSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
