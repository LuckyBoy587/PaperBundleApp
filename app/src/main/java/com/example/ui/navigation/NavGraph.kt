package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.components.FamilyNavIcon
import com.example.ui.components.HomeNavIcon
import com.example.ui.components.SettingsNavIcon
import com.example.ui.components.StitchIndigo
import com.example.ui.components.StitchSlate500
import com.example.ui.screens.family.FamilyScreen
import com.example.ui.screens.family.FamilyViewModel
import com.example.ui.screens.login.LoginScreen
import com.example.ui.screens.login.LoginViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.screens.tasks.TasksViewModel
import com.example.util.UserSession

private fun getTabOrderIndex(route: String?): Int {
    return when (route) {
        Screen.Tasks.route -> 0
        Screen.Family.route -> 1
        Screen.Settings.route -> 2
        else -> -1
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.getTabEnterTransition(): EnterTransition {
    val initialIndex = getTabOrderIndex(initialState.destination.route)
    val targetIndex = getTabOrderIndex(targetState.destination.route)
    return if (initialIndex != -1 && targetIndex != -1) {
        if (initialIndex < targetIndex) {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(tween(300))
        } else {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(tween(300))
        }
    } else {
        fadeIn(tween(300))
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.getTabExitTransition(): ExitTransition {
    val initialIndex = getTabOrderIndex(initialState.destination.route)
    val targetIndex = getTabOrderIndex(targetState.destination.route)
    return if (initialIndex != -1 && targetIndex != -1) {
        if (initialIndex < targetIndex) {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(tween(300))
        } else {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(tween(300))
        }
    } else {
        fadeOut(tween(300))
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    session: UserSession?,
    loginViewModel: LoginViewModel,
    tasksViewModel: TasksViewModel,
    familyViewModel: FamilyViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = if (session == null) Screen.Login.route else Screen.Tasks.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.Login.route,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Tasks.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Tasks.route,
            enterTransition = { getTabEnterTransition() },
            exitTransition = { getTabExitTransition() }
        ) {
            if (session != null) {
                TasksScreen(
                    viewModel = tasksViewModel,
                    session = session
                )
            }
        }

        composable(
            route = Screen.Family.route,
            enterTransition = { getTabEnterTransition() },
            exitTransition = { getTabExitTransition() }
        ) {
            if (session != null) {
                FamilyScreen(
                    viewModel = familyViewModel,
                    session = session,
                    onMemberSelected = {
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { getTabEnterTransition() },
            exitTransition = { getTabExitTransition() }
        ) {
            if (session != null) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    session = session,
                    onNavigateToFamily = {
                        navController.navigate(Screen.Family.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onLogoutComplete = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainBottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .height(64.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item 1: Tasks
            val isTasksActive = currentRoute == Screen.Tasks.route
            val tasksBgColor by animateColorAsState(
                targetValue = if (isTasksActive) StitchIndigo.copy(alpha = 0.12f) else Color.Transparent,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "TasksBgColor"
            )
            val tasksContentColor by animateColorAsState(
                targetValue = if (isTasksActive) StitchIndigo else StitchSlate500,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "TasksContentColor"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tasksBgColor)
                    .clickable { onNavigate(Screen.Tasks.route) }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    HomeNavIcon(tint = tasksContentColor)
                    Text(
                        text = "Tasks",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tasksContentColor
                    )
                }
            }

            // Item 2: Family
            val isFamilyActive = currentRoute == Screen.Family.route
            val familyBgColor by animateColorAsState(
                targetValue = if (isFamilyActive) StitchIndigo.copy(alpha = 0.12f) else Color.Transparent,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "FamilyBgColor"
            )
            val familyContentColor by animateColorAsState(
                targetValue = if (isFamilyActive) StitchIndigo else StitchSlate500,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "FamilyContentColor"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(familyBgColor)
                    .clickable { onNavigate(Screen.Family.route) }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    FamilyNavIcon(tint = familyContentColor)
                    Text(
                        text = "Family",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = familyContentColor
                    )
                }
            }

            // Item 3: Settings
            val isSettingsActive = currentRoute == Screen.Settings.route
            val settingsBgColor by animateColorAsState(
                targetValue = if (isSettingsActive) StitchIndigo.copy(alpha = 0.12f) else Color.Transparent,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "SettingsBgColor"
            )
            val settingsContentColor by animateColorAsState(
                targetValue = if (isSettingsActive) StitchIndigo else StitchSlate500,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "SettingsContentColor"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(settingsBgColor)
                    .clickable { onNavigate(Screen.Settings.route) }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    SettingsNavIcon(tint = settingsContentColor)
                    Text(
                        text = "Settings",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = settingsContentColor
                    )
                }
            }
        }
    }
}
