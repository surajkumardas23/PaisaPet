package com.example.paisapet.ui.onboarding

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// Thin Android glue for S3. The decisions are in onboarding/PermissionCard.kt (pure, tested).

fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** True when Android would still show the dialog after an earlier denial. */
fun shouldShowRationale(activity: Activity, permission: String): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

/** The Activity behind a Compose context, or null. */
fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** This app's App info page. Does nothing if the phone has no such screen. */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    try {
        start(context, intent)
    } catch (e: ActivityNotFoundException) {
        // Nothing else to open.
    }
}

/** This app's notification settings, falling back to App info. */
fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        start(context, intent)
    } catch (e: ActivityNotFoundException) {
        openAppSettings(context)
    }
}

private fun start(context: Context, intent: Intent) {
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
