package com.niloy.focentra

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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.focentra.ui.components.FocentraFloatingNavBar
import com.niloy.focentra.ui.screens.*
import com.niloy.focentra.ui.theme.FocentraTheme
import com.niloy.focentra.viewmodel.MainViewModel
import com.niloy.focentra.viewmodel.NavigationTab
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

            val isBottomBarVisible = remember { mutableStateOf(true) }
            val nestedScrollConnection = remember {
                object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                    override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                        if (available.y < -15f) {
                            isBottomBarVisible.value = false
                        } else if (available.y > 15f) {
                            isBottomBarVisible.value = true
                        }
                        return androidx.compose.ui.geometry.Offset.Zero
                    }
                }
            }

            // Keep screen on during active focus sessions
            DisposableEffect(timerState.status) {
                if (timerState.status == com.niloy.focentra.engine.TimerStatus.RUNNING && timerState.keepScreenAwake) {
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
                        modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    if (currentTab == NavigationTab.DASHBOARD) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primary,
                                                                MaterialTheme.colorScheme.tertiary
                                                            )
                                                        )
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Focentra",
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = (-0.8).sp,
                                                        fontSize = 24.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Deep Focus & Active Recall",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        letterSpacing = 0.2.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    } else {
                                        val isSubPage = currentTab == NavigationTab.SUBJECTS ||
                                                currentTab == NavigationTab.FLASHCARDS ||
                                                currentTab == NavigationTab.ACHIEVEMENTS

                                        Text(
                                            text = when (currentTab) {
                                                NavigationTab.DASHBOARD -> "Focentra"
                                                NavigationTab.TIMER -> "Study Timer"
                                                NavigationTab.ANALYTICS -> "Analytics & Insights"
                                                NavigationTab.CALENDAR -> "Study Calendar"
                                                NavigationTab.HISTORY -> "Session History"
                                                NavigationTab.SUBJECTS -> "Subjects & Topics"
                                                NavigationTab.FLASHCARDS -> "Active Recall Cards"
                                                NavigationTab.ACHIEVEMENTS -> "Achievements"
                                                NavigationTab.PRESETS -> "Study Presets"
                                                NavigationTab.SETTINGS -> "Settings"
                                            },
                                            style = if (isSubPage) {
                                                MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 19.sp,
                                                    letterSpacing = (-0.2).sp
                                                )
                                            } else {
                                                MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = (-0.3).sp
                                                )
                                            }
                                        )
                                    }
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
                                    val isMainScreen = currentTab == NavigationTab.DASHBOARD ||
                                            currentTab == NavigationTab.TIMER ||
                                            currentTab == NavigationTab.ANALYTICS ||
                                            currentTab == NavigationTab.CALENDAR ||
                                            currentTab == NavigationTab.HISTORY ||
                                            currentTab == NavigationTab.SETTINGS ||
                                            currentTab == NavigationTab.PRESETS

                                    if (isMainScreen) {
                                        // Flashcards Quick Access Icon
                                        IconButton(onClick = { viewModel.navigateTo(NavigationTab.FLASHCARDS) }) {
                                            Icon(
                                                imageVector = Icons.Default.Style,
                                                contentDescription = "Flashcards",
                                                tint = if (currentTab == NavigationTab.FLASHCARDS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Subjects Quick Access Icon
                                        IconButton(onClick = { viewModel.navigateTo(NavigationTab.SUBJECTS) }) {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = "Subjects",
                                                tint = if (currentTab == NavigationTab.SUBJECTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Achievements Trophy Icon
                                        IconButton(onClick = { viewModel.navigateTo(NavigationTab.ACHIEVEMENTS) }) {
                                            Icon(
                                                imageVector = Icons.Default.EmojiEvents,
                                                contentDescription = "Achievements",
                                                tint = if (currentTab == NavigationTab.ACHIEVEMENTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // More Menu items
                                    var showMenu by remember { mutableStateOf(false) }
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Active Recall Flashcards") },
                                            leadingIcon = { Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.navigateTo(NavigationTab.FLASHCARDS)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Study Presets") },
                                            leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.navigateTo(NavigationTab.PRESETS)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Settings & Backup") },
                                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
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
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                                },
                                label = "tab_navigation",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = innerPadding.calculateTopPadding())
                            ) { tab ->
                                when (tab) {
                                    NavigationTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                                    NavigationTab.TIMER -> TimerScreen(viewModel = viewModel)
                                    NavigationTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                                    NavigationTab.CALENDAR -> CalendarScreen(viewModel = viewModel)
                                    NavigationTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                                    NavigationTab.SUBJECTS -> SubjectsScreen(viewModel = viewModel)
                                    NavigationTab.FLASHCARDS -> FlashcardsScreen(viewModel = viewModel)
                                    NavigationTab.ACHIEVEMENTS -> AchievementsScreen(viewModel = viewModel)
                                    NavigationTab.PRESETS -> PresetsScreen(viewModel = viewModel)
                                    NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                                }
                            }

                            val shouldShowBottomBar = currentTab != NavigationTab.FLASHCARDS &&
                                    currentTab != NavigationTab.SUBJECTS &&
                                    currentTab != NavigationTab.ACHIEVEMENTS

                            AnimatedVisibility(
                                visible = isBottomBarVisible.value && shouldShowBottomBar,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                FocentraFloatingNavBar(
                                    currentTab = currentTab,
                                    onTabSelected = { tab ->
                                        viewModel.navigateTo(tab)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
