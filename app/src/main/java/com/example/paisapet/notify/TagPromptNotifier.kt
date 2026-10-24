package com.example.paisapet.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.paisapet.MainActivity
import com.example.paisapet.R
import com.example.paisapet.model.Category
import com.example.paisapet.sms.ParsedDebit

/** Posts and removes the "Misc or Essential?" prompt (FR-07). Intents carry only the transaction id and category. */
class TagPromptNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    /** Returns false, posting nothing, when notifications are off for the app or the channel. */
    fun show(id: Long, debit: ParsedDebit): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!manager.areNotificationsEnabled()) return false
        NotificationChannels.create(context)
        if (manager.getNotificationChannelCompat(NotificationChannels.TAG_PROMPTS)?.importance ==
            NotificationManagerCompat.IMPORTANCE_NONE
        ) {
            return false
        }

        val text = tagPromptText(debit.amountPaise, debit.payee)
        val publicVersion = NotificationCompat.Builder(context, NotificationChannels.TAG_PROMPTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(TAG_PROMPT_PUBLIC_TEXT)
            .build()
        val notification = NotificationCompat.Builder(context, NotificationChannels.TAG_PROMPTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text.title)
            .setContentText(text.text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(id))
            .addAction(0, context.getString(R.string.action_misc), tagIntent(id, Category.MISC))
            .addAction(0, context.getString(R.string.action_essential), tagIntent(id, Category.ESSENTIAL))
            .build()
        return try {
            manager.notify(notificationIdFor(id), notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun cancel(id: Long) = manager.cancel(notificationIdFor(id))

    private fun tagIntent(id: Long, category: Category): PendingIntent {
        val intent = Intent(context, TagActionReceiver::class.java)
            .setData(actionUri(id, category).toUri())
            .putExtra(TagActionReceiver.EXTRA_TRANSACTION_ID, id)
            .putExtra(TagActionReceiver.EXTRA_CATEGORY, category.name)
        return PendingIntent.getBroadcast(
            context, requestCodeFor(id, category), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun openAppIntent(id: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setData("paisapet://transaction/$id".toUri())
            .putExtra(EXTRA_OPEN_TRANSACTION_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, notificationIdFor(id), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        /** Read by MainActivity later (S7); ignored for now. */
        const val EXTRA_OPEN_TRANSACTION_ID = "com.example.paisapet.extra.OPEN_TRANSACTION_ID"
    }
}
