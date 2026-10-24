package com.example.paisapet.notify

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.example.paisapet.R

/** The two channels from the PRD "Notifications and widget spec". Creating them again is a cheap no-op. */
object NotificationChannels {
    const val TAG_PROMPTS = "tag_prompts"
    const val BUDGET_ALERTS = "budget_alerts"

    fun create(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(TAG_PROMPTS, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.channel_tag_prompts))
                    .build(),
                NotificationChannelCompat.Builder(BUDGET_ALERTS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.channel_budget_alerts))
                    .build(),
            ),
        )
    }
}
