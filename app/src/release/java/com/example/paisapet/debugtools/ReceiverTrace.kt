package com.example.paisapet.debugtools

import android.content.Context
import com.example.paisapet.sms.PipelineResult

/** Release stub of the debug ReceiverTrace: records nothing. */
data class ReceiverTraceEntry(val kind: String, val detail: String, val timeMillis: Long)

object ReceiverTrace {
    @Suppress("UNUSED_PARAMETER")
    fun record(context: Context, result: PipelineResult) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun read(context: Context): ReceiverTraceEntry? = null
}
