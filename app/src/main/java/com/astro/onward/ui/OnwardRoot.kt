package com.astro.onward.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astro.onward.OnwardApp
import com.astro.onward.data.AppSettings
import com.astro.onward.ui.history.HistoryScreen
import com.astro.onward.ui.onboarding.OnboardingScreen
import com.astro.onward.updates.Updates
import com.astro.onward.ui.plan.PlanScreen
import com.astro.onward.ui.settings.SettingsScreen
import com.astro.onward.ui.sheet.CheatSheetScreen
import com.astro.onward.ui.shopping.ShoppingScreen
import com.astro.onward.ui.today.TodayScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RootViewModel(private val app: OnwardApp) : ViewModel() {
    val settings = app.database.settingsDao().observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Throttled to once a day inside check(); silent when offline.
        viewModelScope.launch { Updates.check(app) }
    }

    fun completeOnboarding(startReminders: Boolean) {
        viewModelScope.launch {
            val current = app.database.settingsDao().get() ?: AppSettings()
            app.database.settingsDao()
                .upsert(current.copy(onboarded = true, started = startReminders))
            app.scheduler.syncAll()
        }
    }
}

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val tabs = listOf(
    Tab("today", "Today", Icons.Outlined.LocalFireDepartment, Icons.Filled.LocalFireDepartment),
    Tab("plan", "Plan", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    Tab("sheet", "Sheet", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    Tab("shopping", "List", Icons.Outlined.Checklist, Icons.Filled.Checklist),
)

@Composable
fun OnwardRoot(
    pendingDestination: StateFlow<String?>,
    onDestinationConsumed: () -> Unit,
) {
    val vm: RootViewModel = viewModel { RootViewModel(this[APPLICATION_KEY] as OnwardApp) }
    val settings by vm.settings.collectAsStateWithLifecycle()

    when {
        settings == null -> Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        !settings!!.onboarded -> OnboardingScreen(onDone = vm::completeOnboarding)
        else -> MainScaffold(pendingDestination, onDestinationConsumed)
    }
}

@Composable
private fun MainScaffold(
    pendingDestination: StateFlow<String?>,
    onDestinationConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val pending by pendingDestination.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        pending?.let { dest ->
            if (tabs.any { it.route == dest }) navigateToTab(dest)
            onDestinationConsumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(padding),
        ) {
            composable("today") {
                TodayScreen(
                    onOpenPlan = { navigateToTab("plan") },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenHistory = { navController.navigate("history") },
                )
            }
            composable("plan") { PlanScreen() }
            composable("sheet") { CheatSheetScreen() }
            composable("shopping") { ShoppingScreen() }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable("history") { HistoryScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
