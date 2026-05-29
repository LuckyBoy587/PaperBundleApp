package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.SyncState
import com.example.ui.components.GlobalSyncStatusBar
import com.example.ui.components.MainHeader
import com.example.ui.components.StitchBg
import com.example.ui.navigation.AppNavHost
import com.example.ui.navigation.MainBottomNavigationBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.family.FamilyViewModel
import com.example.ui.screens.family.FamilyViewModelFactory
import com.example.ui.screens.login.LoginViewModel
import com.example.ui.screens.login.LoginViewModelFactory
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.settings.SettingsViewModelFactory
import com.example.ui.screens.tasks.TasksViewModel
import com.example.ui.screens.tasks.TasksViewModelFactory
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import kotlinx.coroutines.delay

class MainActivityUI {
    @Composable
    fun Render() {
        val context = LocalContext.current
        val app = context.applicationContext as TaskApplication

        // Instantiate isolated screen ViewModels using Compose viewModel() to respect independent lifecycles
        val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(app))
        val tasksViewModel: TasksViewModel = viewModel(factory = TasksViewModelFactory(app, app.repository))
        val familyViewModel: FamilyViewModel = viewModel(factory = FamilyViewModelFactory(app))
        val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(app, app.updateRepository))

        val navController = rememberNavController()
        val userSession by tasksViewModel.currentUserSession.collectAsState()
        val familyMembers by tasksViewModel.familyMembers.collectAsState()
        val curProfile by tasksViewModel.curProfile.collectAsState()

        // Handle global session state transitions (e.g. login & logout)
        LaunchedEffect(userSession) {
            if (userSession == null) {
                // Clear back stack and return to Log in Screen on logout
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                // If on Login page and session becomes active, route to Tasks
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute == Screen.Login.route || currentRoute == null) {
                    navController.navigate(Screen.Tasks.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute in listOf(Screen.Tasks.route, Screen.Family.route, Screen.Settings.route)
        val contentBottomPadding = if (showBottomBar) 16.dp else 0.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StitchBg)
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    if (showBottomBar && userSession != null) {
                        val session = userSession!!
                        val activeMember = familyMembers.find { it.uid == curProfile }
                        val initialLetter = (activeMember?.name ?: session.name).take(1).uppercase()
                        val language by tasksViewModel.curLanguage.collectAsState()

                        val isNetworkAvailable = remember { mutableStateOf(FirebaseSyncManager.isNetworkAvailable()) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                isNetworkAvailable.value = FirebaseSyncManager.isNetworkAvailable()
                                delay(3000)
                            }
                        }

                        val activeTasks by tasksViewModel.tasks.collectAsState()
                        val hasPendingWrites = remember(activeTasks) {
                            activeTasks.any { it.syncState == SyncState.PENDING_WRITE || it.syncState == SyncState.SYNCING }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.06f),
                                        spotColor = Color.Black.copy(alpha = 0.08f)
                                    )
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                MainHeader(
                                    initial = initialLetter,
                                    language = language,
                                    onAvatarClick = {
                                        navController.navigate(Screen.Family.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onLanguageClick = {
                                        val nextLanguage = if (language == Language.EN) Language.TA else Language.EN
                                        tasksViewModel.setLanguage(nextLanguage)
                                        loginViewModel.curLanguage.value = nextLanguage
                                        familyViewModel.curLanguage.value = nextLanguage
                                        settingsViewModel.curLanguage.value = nextLanguage
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            GlobalSyncStatusBar(
                                isNetworkAvailable = isNetworkAvailable.value,
                                hasPendingWrites = hasPendingWrites,
                                isFirebaseInitialized = FirebaseSyncManager.isFirebaseInitialized
                            )
                        }
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MainBottomNavigationBar(
                                navController = navController,
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        // Save and restore tab state to support standard Bottom Nav preservation
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    session = userSession,
                    loginViewModel = loginViewModel,
                    tasksViewModel = tasksViewModel,
                    familyViewModel = familyViewModel,
                    settingsViewModel = settingsViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(bottom = contentBottomPadding)
                )
            }
        }
    }
}
