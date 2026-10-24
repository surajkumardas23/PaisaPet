package com.example.paisapet.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.paisapet.PaisaPetApp
import com.example.paisapet.debugtools.ReceiverTrace
import kotlinx.coroutines.launch

/** Bank SMS in, even with the app closed (FR-04). All work runs off the main thread; nothing is logged. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        try {
            val appContext = context.applicationContext
            val container = (appContext as PaisaPetApp).container
            container.appScope.launch {
                try {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)?.filterNotNull().orEmpty()
                    val now = System.currentTimeMillis()
                    val parts = messages.map {
                        SmsPart(it.originatingAddress, it.messageBody, receivedAtMillis(it.timestampMillis, now))
                    }
                    for (sms in joinParts(parts)) {
                        try {
                            val result = container.smsPipeline.process(sms)
                            ReceiverTrace.record(appContext, result)
                        } catch (t: Throwable) {
                            // One bad message must not stop the others (NFR-04).
                        }
                    }
                } catch (t: Throwable) {
                    // Never crash (NFR-04).
                } finally {
                    pending.finish()
                }
            }
        } catch (t: Throwable) {
            pending.finish()
        }
    }
}
