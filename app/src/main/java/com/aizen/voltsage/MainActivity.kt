package com.aizen.voltsage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.aizen.voltsage.ui.Screen
import com.aizen.voltsage.ui.VoltSageViewModel
import com.aizen.voltsage.ui.screens.AnalyticsScreen
import com.aizen.voltsage.ui.screens.CalendarScreen
import com.aizen.voltsage.ui.screens.HomeScreen
import com.aizen.voltsage.ui.screens.QuizScreen
import com.aizen.voltsage.ui.screens.StudyDetailScreen
import com.aizen.voltsage.ui.screens.StudyGroupsScreen
import com.aizen.voltsage.ui.theme.VoltAmber
import com.aizen.voltsage.ui.theme.VoltCyan
import com.aizen.voltsage.ui.theme.VoltSageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase App Check if a valid Firebase configuration (google-services.json) exists
        try {
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                val appCheck = FirebaseAppCheck.getInstance()
                val providerFactory = DebugAppCheckProviderFactory.getInstance()
                appCheck.installAppCheckProviderFactory(providerFactory)
            }
        } catch (e: Exception) {
            android.util.Log.w("VoltSage", "Firebase AppCheck notice: ${e.message}")
        }

        setContent {
            VoltSageApp()
        }
    }
}

@Composable
fun VoltSageApp(viewModel: VoltSageViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isAmoled by viewModel.isAmoledTheme.collectAsState()

    VoltSageTheme(amoledTheme = isAmoled) {
    // Handle back button on sub-screens
    BackHandler(enabled = currentScreen !is Screen.Home) {
        viewModel.navigateTo(Screen.Home)
    }

    val showBottomBar = currentScreen !is Screen.Quiz

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    val isHome = currentScreen is Screen.Home || currentScreen is Screen.Detail
                    val isCalendar = currentScreen is Screen.Calendar
                    val isGroups = currentScreen is Screen.Groups
                    val isAnalytics = currentScreen is Screen.Analytics

                    NavigationBarItem(
                        selected = isHome,
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        icon = {
                            Icon(
                                imageVector = if (isHome) Icons.Filled.Bolt else Icons.Outlined.Bolt,
                                contentDescription = "Study"
                            )
                        },
                        label = {
                            Text("Study", fontSize = 11.sp, fontWeight = if (isHome) FontWeight.Bold else FontWeight.Normal)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoltAmber,
                            selectedTextColor = VoltAmber,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_study")
                    )

                    NavigationBarItem(
                        selected = isCalendar,
                        onClick = { viewModel.navigateTo(Screen.Calendar) },
                        icon = {
                            Icon(
                                imageVector = if (isCalendar) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                contentDescription = "Calendar"
                            )
                        },
                        label = {
                            Text("Calendar", fontSize = 11.sp, fontWeight = if (isCalendar) FontWeight.Bold else FontWeight.Normal)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoltCyan,
                            selectedTextColor = VoltCyan,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_calendar")
                    )

                    NavigationBarItem(
                        selected = isGroups,
                        onClick = { viewModel.navigateTo(Screen.Groups) },
                        icon = {
                            Icon(
                                imageVector = if (isGroups) Icons.Filled.Groups else Icons.Outlined.Groups,
                                contentDescription = "Squads"
                            )
                        },
                        label = {
                            Text("Squads", fontSize = 11.sp, fontWeight = if (isGroups) FontWeight.Bold else FontWeight.Normal)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoltCyan,
                            selectedTextColor = VoltCyan,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_groups")
                    )

                    NavigationBarItem(
                        selected = isAnalytics,
                        onClick = { viewModel.navigateTo(Screen.Analytics) },
                        icon = {
                            Icon(
                                imageVector = if (isAnalytics) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                contentDescription = "Analytics"
                            )
                        },
                        label = {
                            Text("Analytics", fontSize = 11.sp, fontWeight = if (isAnalytics) FontWeight.Bold else FontWeight.Normal)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoltAmber,
                            selectedTextColor = VoltAmber,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_analytics")
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(innerPadding),
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToPack = { viewModel.navigateTo(Screen.Detail(it)) },
                        onNavigateToQuiz = { viewModel.navigateTo(Screen.Quiz(it)) }
                    )
                }
                is Screen.Detail -> {
                    StudyDetailScreen(
                        packId = screen.packId,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.Home) },
                        onStartQuiz = { viewModel.navigateTo(Screen.Quiz(it)) },
                        onOpenCalendar = { viewModel.navigateTo(Screen.Calendar) },
                        onOpenGroups = { viewModel.navigateTo(Screen.Groups) }
                    )
                }
                is Screen.Quiz -> {
                    QuizScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.Home) },
                        onViewNotes = { viewModel.navigateTo(Screen.Detail(it)) }
                    )
                }
                is Screen.Calendar -> {
                    CalendarScreen(
                        viewModel = viewModel,
                        onLaunchQuiz = { viewModel.navigateTo(Screen.Quiz(it)) },
                        onLaunchNotes = { viewModel.navigateTo(Screen.Detail(it)) }
                    )
                }
                is Screen.Groups -> {
                    StudyGroupsScreen(viewModel = viewModel)
                }
                is Screen.Analytics -> {
                    AnalyticsScreen(viewModel = viewModel)
                }
                is Screen.Settings -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToPack = { viewModel.navigateTo(Screen.Detail(it)) },
                        onNavigateToQuiz = { viewModel.navigateTo(Screen.Quiz(it)) }
                    )
                }
            }
        }
    }
}
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

