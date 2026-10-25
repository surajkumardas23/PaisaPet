package com.example.paisapet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.paisapet.nav.NO_OPEN_ID
import com.example.paisapet.nav.OpenRequest
import com.example.paisapet.nav.parseOpenId
import com.example.paisapet.nav.readsOpenExtra
import com.example.paisapet.notify.TagPromptNotifier
import com.example.paisapet.ui.AppRoot
import com.example.paisapet.ui.theme.PaisaPetTheme

class MainActivity : ComponentActivity() {

    /** A notification body tap waiting to open the Edit sheet; AppRoot consumes it. */
    private var openRequest by mutableStateOf<OpenRequest?>(null)
    private var openSeq = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is light-only for now, so keep dark system-bar icons even when the phone is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(LIGHT_SCRIM, DARK_SCRIM),
        )
        val launchedFromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (readsOpenExtra(restored = savedInstanceState != null, launchedFromHistory = launchedFromHistory)) {
            takeOpenRequest(intent)
        }
        val container = (application as PaisaPetApp).container
        setContent {
            PaisaPetTheme {
                AppRoot(
                    container = container,
                    openRequest = openRequest,
                    onOpenConsumed = { done -> if (openRequest == done) openRequest = null },
                )
            }
        }
    }

    /** singleTop: a body tap while the app is running arrives here instead of recreating the activity. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeOpenRequest(intent)
    }

    /** Reads the payment id once, then removes it so the same intent can never open the sheet again. */
    private fun takeOpenRequest(intent: Intent) {
        val id = parseOpenId(intent.getLongExtra(TagPromptNotifier.EXTRA_OPEN_TRANSACTION_ID, NO_OPEN_ID))
        intent.removeExtra(TagPromptNotifier.EXTRA_OPEN_TRANSACTION_ID)
        if (id != null) openRequest = OpenRequest(id, ++openSeq)
    }

    private companion object {
        // The activity library's defaults: the scrim behind 3-button navigation, and the darker one used
        // where the system cannot draw dark navigation icons.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
