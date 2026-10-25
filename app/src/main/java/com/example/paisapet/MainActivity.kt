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
import com.example.paisapet.nav.EXTRA_OPEN_TARGET
import com.example.paisapet.nav.NO_OPEN_ID
import com.example.paisapet.nav.OpenRequest
import com.example.paisapet.nav.parseOpenTarget
import com.example.paisapet.nav.readsOpenExtra
import com.example.paisapet.notify.TagPromptNotifier
import com.example.paisapet.ui.AppRoot
import com.example.paisapet.ui.theme.PaisaPetTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** A notification or widget tap waiting to be applied; AppRoot consumes it. */
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

    /** Safety net: the widgets catch up with anything they missed. A no-op when no widget is placed. */
    override fun onStart() {
        super.onStart()
        val container = (application as PaisaPetApp).container
        container.appScope.launch { container.widgetRefresher.refreshIfPlaced() }
    }

    /** singleTop: a tap while the app is running arrives here instead of recreating the activity. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeOpenRequest(intent)
    }

    /** Reads the open extras once, then removes both so the same intent can never be applied again. */
    private fun takeOpenRequest(intent: Intent) {
        val target = parseOpenTarget(
            intent.getLongExtra(TagPromptNotifier.EXTRA_OPEN_TRANSACTION_ID, NO_OPEN_ID),
            intent.getStringExtra(EXTRA_OPEN_TARGET),
        )
        if (target != null) openRequest = OpenRequest(target, ++openSeq)
        intent.removeExtra(TagPromptNotifier.EXTRA_OPEN_TRANSACTION_ID)
        intent.removeExtra(EXTRA_OPEN_TARGET)
    }

    private companion object {
        // The activity library's defaults: the scrim behind 3-button navigation, and the darker one used
        // where the system cannot draw dark navigation icons.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
