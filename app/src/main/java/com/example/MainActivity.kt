package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.repository.VitalPrepRepository
import com.example.notifications.ReminderScheduler
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.WellnessScreen
import com.example.ui.theme.VitalPrepTheme
import com.example.ui.viewmodel.VitalPrepViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object Routes {
  const val DASHBOARD = "dashboard"
  const val FOCUS = "focus"
  const val WELLNESS = "wellness"
  const val PROGRESS = "progress"
  const val REMINDERS = "reminders"
}

private data class NavTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

private val appTabs =
  listOf(
    NavTab(Routes.DASHBOARD, "Today", Icons.Filled.Home, Icons.Outlined.Home),
    NavTab(Routes.FOCUS, "Focus", Icons.Filled.Timer, Icons.Outlined.Timer),
    NavTab(Routes.WELLNESS, "Wellness", Icons.Filled.SelfImprovement, Icons.Outlined.SelfImprovement),
    NavTab(
      Routes.PROGRESS,
      "Progress",
      Icons.AutoMirrored.Filled.ShowChart,
      Icons.AutoMirrored.Outlined.ShowChart,
    ),
    NavTab(
      Routes.REMINDERS,
      "Cues",
      Icons.Filled.NotificationsActive,
      Icons.Outlined.NotificationsActive,
    ),
  )

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ReminderScheduler.ensureChannel(this)
    enableEdgeToEdge()
    setContent {
      VitalPrepTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      VitalPrepAppShell(currentUser = user)
    }
  }
}

@Composable
private fun VitalPrepAppShell(currentUser: FirebaseUser) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: VitalPrepViewModel =
    viewModel(
      key = currentUser.uid,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            VitalPrepViewModel(
              repository = VitalPrepRepository(db),
              currentUserId = currentUser.uid,
              initialDisplayName = currentUser.displayName ?: "Aspirant",
            )
          }
        },
    )

  val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
  val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
  val dailyLog by viewModel.selectedDailyLog.collectAsStateWithLifecycle()
  val sessions by viewModel.sessionsForSelectedDate.collectAsStateWithLifecycle()
  val meals by viewModel.mealsForSelectedDate.collectAsStateWithLifecycle()
  val recentLogs by viewModel.recentDailyLogs.collectAsStateWithLifecycle()
  val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
  val reminders by viewModel.reminders.collectAsStateWithLifecycle()
  val score by viewModel.dashboardScore.collectAsStateWithLifecycle()
  val timerState by viewModel.timerState.collectAsStateWithLifecycle()
  val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
  val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(reminders) {
    ReminderScheduler.syncReminders(context, reminders)
  }

  LaunchedEffect(statusMessage, errorMessage) {
    val msg = errorMessage ?: statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessages()
    }
  }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: Routes.DASHBOARD

  val navigateToTab: (String) -> Unit = { route ->
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.statusBars,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useNavRail) {
          NavigationBar {
            appTabs.forEach { tab ->
              val selected = currentRoute == tab.route
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label, maxLines = 1) },
                modifier = Modifier.testTag("nav_${tab.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavRail) {
          NavigationRail {
            appTabs.forEach { tab ->
              val selected = currentRoute == tab.route
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("nav_${tab.route}"),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
          ) {
            composable(Routes.DASHBOARD) {
              DashboardScreen(
                selectedDateKey = selectedDateKey,
                settings = userSettings,
                dailyLog = dailyLog,
                score = score,
                sessions = sessions,
                reminders = reminders,
                onShiftDate = viewModel::shiftSelectedDate,
                onStartFocusCategory = { cat ->
                  viewModel.selectTimerCategory(cat)
                  navigateToTab(Routes.FOCUS)
                },
                onOpenWellness = { navigateToTab(Routes.WELLNESS) },
                onOpenReminders = { navigateToTab(Routes.REMINDERS) },
                onDeleteSession = viewModel::deleteSession,
              )
            }

            composable(Routes.FOCUS) {
              FocusScreen(
                selectedDateKey = selectedDateKey,
                timerState = timerState,
                sessions = sessions,
                onSelectCategory = viewModel::selectTimerCategory,
                onSetDuration = viewModel::setTimerDurationMinutes,
                onSelectTag = viewModel::updateTimerTag,
                onTopicChange = viewModel::updateTimerTopic,
                onToggleTimer = viewModel::toggleTimer,
                onResetTimer = viewModel::resetTimer,
                onCompleteAndSaveTimer = viewModel::completeTimerAndSave,
                onLogManualSession = viewModel::logManualSession,
                onDeleteSession = viewModel::deleteSession,
              )
            }

            composable(Routes.WELLNESS) {
              WellnessScreen(
                selectedDateKey = selectedDateKey,
                settings = userSettings,
                dailyLog = dailyLog,
                meals = meals,
                onSaveSleep = viewModel::saveSleep,
                onAdjustWater = viewModel::adjustWater,
                onAddMeal = viewModel::addMeal,
                onDeleteMeal = viewModel::deleteMeal,
              )
            }

            composable(Routes.PROGRESS) {
              ProgressScreen(
                settings = userSettings,
                recentLogs = recentLogs,
                recentSessions = recentSessions,
                streakDays = score.currentStreakDays,
                onSaveTargets = viewModel::saveTargets,
              )
            }

            composable(Routes.REMINDERS) {
              RemindersScreen(
                userDisplayName = currentUser.displayName ?: userSettings.displayName,
                userEmail = currentUser.email,
                reminders = reminders,
                onSaveReminder = viewModel::saveReminder,
                onToggleReminder = viewModel::toggleReminder,
                onDeleteReminder = viewModel::deleteReminder,
                onSignOut = {
                  signOutUser(credentialManager = credentialManager, scope = scope)
                },
              )
            }
          }
        }
      }
    }
  }
}
