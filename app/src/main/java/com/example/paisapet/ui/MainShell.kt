package com.example.paisapet.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paisapet.AppContainer
import com.example.paisapet.R
import com.example.paisapet.debugtools.DebugTools
import com.example.paisapet.list.effectiveMonth
import com.example.paisapet.list.monthOptions
import com.example.paisapet.nav.NavState
import com.example.paisapet.nav.OpenRequest
import com.example.paisapet.nav.Sheet
import com.example.paisapet.nav.Tab
import com.example.paisapet.nav.back
import com.example.paisapet.nav.navStateFromSaveable
import com.example.paisapet.nav.openTransaction
import com.example.paisapet.nav.seeAll
import com.example.paisapet.nav.tagNow
import com.example.paisapet.nav.toSaveable
import com.example.paisapet.ui.common.rememberToday
import com.example.paisapet.ui.edit.AddExpenseSheet
import com.example.paisapet.ui.edit.EditSheet
import com.example.paisapet.ui.home.HomeScreen
import com.example.paisapet.ui.home.buildHomeModel
import com.example.paisapet.ui.home.showAutoTrackingBanner
import com.example.paisapet.ui.onboarding.isGranted
import com.example.paisapet.ui.onboarding.openAppSettings
import com.example.paisapet.ui.settings.BudgetSheet
import com.example.paisapet.ui.settings.SettingsScreen
import com.example.paisapet.ui.transactions.TransactionsScreen
import com.example.paisapet.ui.transactions.buildTransactionsModel
import com.example.paisapet.ui.theme.CardWhite
import kotlinx.coroutines.launch
import java.time.YearMonth

private val NavStateSaver = listSaver<NavState, Any?>(
    save = { it.toSaveable() },
    restore = { navStateFromSaveable(it) },
)

/** The app after onboarding: Home and Transactions tabs, the Settings page and (debug) Simulate SMS. */
@Composable
fun MainShell(
    container: AppContainer,
    openRequest: OpenRequest? = null,
    onOpenConsumed: (OpenRequest) -> Unit = {},
) {
    var nav by rememberSaveable(stateSaver = NavStateSaver) { mutableStateOf(NavState()) }
    BackHandler(enabled = nav.back() != null) { nav.back()?.let { nav = it } }

    // A notification body tap: open that payment's Edit sheet over the current tab, exactly once.
    // NavState then keeps the sheet across rotation and process death, so the request is not needed again.
    LaunchedEffect(openRequest) {
        val request = openRequest ?: return@LaunchedEffect
        nav = nav.openTransaction(request.id)
        onOpenConsumed(request)
    }

    // The zone is read now, not kept from start-up, so a time zone change applies at once.
    val zone = container.zone
    val today = rememberToday(container.clock)
    val currentKey = YearMonth.from(today).toString()
    val keysWithData by remember { container.repository.observeMonthsWithData() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val options = monthOptions(keysWithData, currentKey)
    val monthKey = effectiveMonth(nav.monthKey, options, currentKey)
    // The current month is kept as null so it keeps following the clock after midnight on the 1st.
    val selectMonth: (String) -> Unit = { key -> nav = nav.copy(monthKey = key.takeUnless { it == currentKey }) }

    // Held here, not in the tabs, so each tab keeps its scroll position when the other is shown.
    val homeScroll = rememberScrollState()
    val transactionsList = rememberLazyListState()
    // A new filter or month starts the list at the top. requestScrollToItem also works while the tab is hidden.
    LaunchedEffect(nav.filter, monthKey) { transactionsList.requestScrollToItem(0) }

    // One month view for both tabs: the same (month, day) is collected once.
    val view by remember(monthKey, today) { container.repository.observeMonth(monthKey, today) }
        .collectAsStateWithLifecycle(initialValue = null)

    // The current month's budget for Settings and the budget sheet, whichever month the tabs show.
    val currentView by remember(currentKey, today) { container.repository.observeMonth(currentKey, today) }
        .collectAsStateWithLifecycle(initialValue = null)
    val currentBudgetPaise = currentView?.summary?.budgetPaise

    // FR-03: re-checked every time the app comes back to the front, e.g. from the system settings page.
    val context = LocalContext.current
    var smsGranted by remember { mutableStateOf(isGranted(context, Manifest.permission.RECEIVE_SMS)) }
    LifecycleResumeEffect(Unit) {
        smsGranted = isGranted(context, Manifest.permission.RECEIVE_SMS)
        onPauseOrDispose { }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val openTransaction: (Long) -> Unit = { id -> nav = nav.openTransaction(id) }
    val editBudget: () -> Unit = { nav = nav.copy(sheet = Sheet.Budget) }

    when {
        nav.debugOpen && DebugTools.isAvailable -> PushedPage(title = DebugTools.TITLE, onBack = { nav = nav.copy(debugOpen = false) }) {
            DebugTools.Screen(container)
        }
        nav.settingsOpen -> PushedPage(title = "Settings", onBack = { nav = nav.copy(settingsOpen = false) }) {
            SettingsScreen(
                budgetPaise = currentBudgetPaise,
                onEditBudget = editBudget,
                onOpenDebug = { nav = nav.copy(debugOpen = true) },
            )
        }
        else -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(containerColor = CardWhite) {
                    NavigationBarItem(
                        selected = nav.tab == Tab.HOME,
                        onClick = { nav = nav.copy(tab = Tab.HOME) },
                        icon = { Icon(painterResource(R.drawable.ic_home), contentDescription = null) },
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = nav.tab == Tab.TRANSACTIONS,
                        onClick = { nav = nav.copy(tab = Tab.TRANSACTIONS) },
                        icon = { Icon(painterResource(R.drawable.ic_receipt), contentDescription = null) },
                        label = { Text("Transactions") },
                    )
                }
            },
            // Past months are read-only, so they get no "+ Add expense".
            floatingActionButton = {
                if (view != null && monthKey == currentKey) {
                    ExtendedFloatingActionButton(
                        onClick = { nav = nav.copy(sheet = Sheet.Add) },
                        icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                        text = { Text("Add expense") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            },
        ) { padding ->
            val loaded = view
            when {
                loaded == null -> Box(Modifier.fillMaxSize().padding(padding))
                nav.tab == Tab.HOME -> HomeScreen(
                    model = buildHomeModel(loaded, monthKey, currentKey, today, zone),
                    monthOptions = options,
                    currentKey = currentKey,
                    onSelectMonth = selectMonth,
                    onOpenSettings = { nav = nav.copy(settingsOpen = true) },
                    onTagNow = { nav = nav.tagNow() },
                    onSeeAll = { nav = nav.seeAll() },
                    onRowClick = openTransaction,
                    onSetBudget = editBudget,
                    contentPadding = padding,
                    scrollState = homeScroll,
                    // MainShell only runs after onboarding, so onboarding is complete here.
                    showAutoTrackingOff = showAutoTrackingBanner(onboardingComplete = true, smsGranted = smsGranted),
                    onOpenAppSettings = { openAppSettings(context) },
                )
                else -> TransactionsScreen(
                    model = buildTransactionsModel(loaded, nav.filter, monthKey, currentKey, today, zone),
                    monthOptions = options,
                    currentKey = currentKey,
                    onSelectMonth = selectMonth,
                    onSelectFilter = { nav = nav.copy(filter = it) },
                    onRowClick = openTransaction,
                    contentPadding = padding,
                    listState = transactionsList,
                )
            }
        }
    }

    if (nav.sheet == Sheet.Budget) {
        BudgetSheet(
            currentBudgetPaise = currentBudgetPaise,
            onSave = container.repository::setBudget,
            onClose = { nav = nav.copy(sheet = Sheet.None) },
        )
    }
    if (nav.sheet == Sheet.Add) {
        AddExpenseSheet(
            clock = container.clock,
            onAdd = container.paymentActions::add,
            onClose = { nav = nav.copy(sheet = Sheet.None) },
        )
    }
    (nav.sheet as? Sheet.Edit)?.let { edit ->
        // Keyed by id: a tap on another payment's notification starts a fresh sheet, not the old one's state.
        key(edit.id) {
            EditSheet(
                id = edit.id,
                container = container,
                currentKey = currentKey,
                onClose = { nav = nav.copy(sheet = Sheet.None) },
                onDeleted = {
                    nav = nav.copy(sheet = Sheet.None)
                    scope.launch { snackbarHostState.showSnackbar("This payment was deleted") }
                },
            )
        }
    }
}

/** A full-screen page with a back arrow, pushed over the tabs. */
@Composable
private fun PushedPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
            }
            Spacer(Modifier.width(4.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}
