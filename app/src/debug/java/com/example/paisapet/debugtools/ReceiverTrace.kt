package com.example.paisapet.debugtools

import android.content.Context
import androidx.core.content.edit
import com.example.paisapet.sms.PipelineResult

/** The last SMS the receiver handled, as outcome only. Never the body or the sender. */
data class ReceiverTraceEntry(val kind: String, val detail: String, val timeMillis: Long)

/** Debug build: remembers the receiver's last outcome for the Simulate SMS screen. */
object ReceiverTrace {
    private const val PREFS = "receiver_trace"
    private const val KEY_KIND = "kind"
    private const val KEY_DETAIL = "detail"
    private const val KEY_TIME = "time"

    fun record(context: Context, result: PipelineResult) {
        val (kind, detail) = when (result) {
            is PipelineResult.Saved -> "Saved" to if (result.prompted) "prompted" else "not prompted"
            PipelineResult.Duplicate -> "Duplicate" to ""
            is PipelineResult.Ignored -> "Ignored" to result.reason.code
            is PipelineResult.Failed -> "Failed" to result.stage.name
        }
        // commit(): the receiver's process may be killed right after finish().
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(commit = true) {
            putString(KEY_KIND, kind)
            putString(KEY_DETAIL, detail)
            putLong(KEY_TIME, System.currentTimeMillis())
        }
    }

    fun read(context: Context): ReceiverTraceEntry? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val kind = prefs.getString(KEY_KIND, null) ?: return null
        return ReceiverTraceEntry(kind, prefs.getString(KEY_DETAIL, "").orEmpty(), prefs.getLong(KEY_TIME, 0))
    }
}
