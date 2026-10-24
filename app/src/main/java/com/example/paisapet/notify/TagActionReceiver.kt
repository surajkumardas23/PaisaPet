package com.example.paisapet.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.paisapet.PaisaPetApp
import kotlinx.coroutines.launch

/** Misc / Essential tap on the tag prompt. Not exported: only our own PendingIntents reach it. */
class TagActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        try {
            val id = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
            val category = intent.getStringExtra(EXTRA_CATEGORY)
            val container = (context.applicationContext as PaisaPetApp).container
            container.appScope.launch {
                try {
                    container.tagActionHandler.handle(id, category)
                } catch (t: Throwable) {
                    // Never crash (NFR-04); the prompt stays so the user can retry.
                } finally {
                    pending.finish()
                }
            }
        } catch (t: Throwable) {
            pending.finish()
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "com.example.paisapet.extra.TRANSACTION_ID"
        const val EXTRA_CATEGORY = "com.example.paisapet.extra.CATEGORY"
    }
}
