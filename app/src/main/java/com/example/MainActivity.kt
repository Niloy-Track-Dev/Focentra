package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.FocentraTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.NavigationTab
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
            val isFullScreenFocus by viewModel.isFullScreenFocus.collectAsStateWithLifecycle()
            val timerState by viewModel.timerEngine.uiState.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            // Keep screen on during active focus sessions
            DisposableEffect(timerState.status) {
                if (timerState.status == com.example.engine.TimerStatus.RUNNING && timerState.keepScreenAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Notification permission request for Android 13+
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                viewModel.snackbarMessage.collectLatest { msg ->
                    val result = snackbarHostState.showSnackbar(
                        message = msg,
                        actionLabel = if (msg.contains("deleted", ignoreCase = true)) "Undo" else null,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteSession()
                    }
                }
            }

            FocentraTheme(themeName = currentTheme) {
                if (isFullScreenFocus) {
                    FullScreenFocusScreen(viewModel = viewModel)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = when (currentTab) {
                                            NavigationTab.DASHBOARD -> "Focentra"
                                            NavigationTab.TIMER -> "Study Timer"
                                            NavigationTab.ANALYTICS -> "Analytics & Insights"
                                            NavigationTab.CALENDAR -> "Study Calendar"
                                            NavigationTab.HISTORY -> "Session History"
                                            NavigationTab.SUBJECTS -> "Subjects & Topics"
                                            NavigationTab.ACHIEVEMENTS -> "Achievements"
                                            NavigationTab.PRESETS -> "Study Presets"
                                            NavigationTab.SETTINGS -> "Settings"
                                        },
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                },
                                navigationIcon = {
                                    if (currentTab != NavigationTab.DASHBOARD &&
                                        currentTab != NavigationTab.TIMER &&
                                        currentTab != NavigationTab.ANALYTICS &&
                                        currentTab != NavigationTab.CALENDAR &&
                                        currentTab != NavigationTab.HISTORY
                                    ) {
                                        IconButton(onClick = { viewModel.navigateTo(NavigationTab.DASHBOARD) }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back"
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    // More Menu items
                                    var showMenu by remember { mutableStateOf(false) }
                                    IconButton(onClick = { viewModel.navigateTo(NavigationTab.SUBJECTS) }) {
                                        Icon(Icons.Default.School, contentDescription = "Subjects")
                                    }
                                    IconButton(onClick = { viewModel.navigateTo(NavigationTab.ACHIEVEMENTS) }) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements")
                                    }
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Study Presets") },
                                            leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.navigateTo(NavigationTab.PRESETS)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Settings & Backup") },
                                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.navigateTo(NavigationTab.SETTINGS)
                                            }
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 2.dp,
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = androidx.compose.ui.graphics.RectangleShape
                                )
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == NavigationTab.DASHBOARD,
                                    onClick = { viewModel.navigateTo(NavigationTab.DASHBOARD) },
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                    label = { Text("Home", fontWeight = if (currentTab == NavigationTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == NavigationTab.TIMER,
                                    onClick = { viewModel.navigateTo(NavigationTab.TIMER) },
                                    icon = { Icon(Icons.Default.Timer, contentDescription = "Timer") },
                                    label = { Text("Timer", fontWeight = if (currentTab == NavigationTab.TIMER) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == NavigationTab.ANALYTICS,
                                    onClick = { viewModel.navigateTo(NavigationTab.ANALYTICS) },
                                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                                    label = { Text("Stats", fontWeight = if (currentTab == NavigationTab.ANALYTICS) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == NavigationTab.CALENDAR,
                                    onClick = { viewModel.navigateTo(NavigationTab.CALENDAR) },
                                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Calendar") },
                                    label = { Text("History", fontWeight = if (currentTab == NavigationTab.CALENDAR) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == NavigationTab.SETTINGS,
                                    onClick = { viewModel.navigateTo(NavigationTab.SETTINGS) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings", fontWeight = if (currentTab == NavigationTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        AnimatedContent(
                            targetState = currentTab,
                            label = "tab_navigation",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) { tab ->
                            when (tab) {
                                NavigationTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                                NavigationTab.TIMER -> TimerScreen(viewModel = viewModel)
                                NavigationTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                                NavigationTab.CALENDAR -> CalendarScreen(viewModel = viewModel)
                                NavigationTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                                NavigationTab.SUBJECTS -> SubjectsScreen(viewModel = viewModel)
                                NavigationTab.ACHIEVEMENTS -> AchievementsScreen(viewModel = viewModel)
                                NavigationTab.PRESETS -> PresetsScreen(viewModel = viewModel)
                                NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
