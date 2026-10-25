package com.example.paisapet.refresh

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What runs after every payment, tag, edit, delete, add, budget change or onboarding change.
 * One run at a time (Mutex), so two changes a second apart never interleave: the second sees the first's result.
 * Step 10 adds the limit alerts as more constructor parameters with do-nothing defaults, run in this order
 * under the same lock: read the month → decide → write flags → post alert → refresh widgets.
 */
class AfterDataChange(
    private val refreshWidgets: suspend () -> Unit,
) {
    private val mutex = Mutex()

    /**
     * [heldBackId]: a payment whose tag prompt was just shown; alerts will leave it out (step 10).
     * Never throws, except [CancellationException].
     */
    suspend fun run(heldBackId: Long? = null) {
        try {
            mutex.withLock {
                refreshWidgets()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            // The data is saved; screens observe Room and the widgets catch up on the next run.
        }
    }
}
