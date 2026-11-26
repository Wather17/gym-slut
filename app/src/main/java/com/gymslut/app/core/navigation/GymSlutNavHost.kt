package com.gymslut.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymslut.app.R
import com.gymslut.app.feature.catalog.CatalogScreen
import com.gymslut.app.feature.plans.PlanDetailScreen
import com.gymslut.app.feature.plans.PlansScreen
import com.gymslut.app.feature.plans.WorkoutEditorScreen
import com.gymslut.app.feature.today.TodayScreen

/**
 * Shell principal com barra de navegação inferior de 4 abas e rotas secundárias.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymSlutAppShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isTopLevelTab = BottomNavTabs.any { it.destination.route == currentRoute }

    val topBarTitle = when (currentRoute) {
        AppDestination.Today.route -> stringResource(R.string.nav_today)
        AppDestination.Plans.route -> stringResource(R.string.nav_plans)
        AppDestination.Catalog.route -> stringResource(R.string.nav_catalog)
        AppDestination.History.route -> stringResource(R.string.nav_history)
        AppDestination.Settings.route -> stringResource(R.string.nav_settings)
        AppDestination.PlanDetail.route -> stringResource(R.string.nav_plan_detail)
        AppDestination.WorkoutEditor.route -> stringResource(R.string.nav_workout_editor)
        AppDestination.ActiveWorkout.route -> stringResource(R.string.nav_active_workout)
        else -> stringResource(R.string.app_name)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = topBarTitle) },
                navigationIcon = {
                    if (!isTopLevelTab) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                },
                actions = {
                    if (isTopLevelTab) {
                        IconButton(onClick = { navController.navigate(AppDestination.Settings.route) }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.nav_settings)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isTopLevelTab) {
                NavigationBar {
                    BottomNavTabs.forEach { tab ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == tab.destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = { Text(text = stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Today.route) {
                TodayScreen(
                    onNavigateToPlans = {
                        navController.navigate(AppDestination.Plans.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenActiveSession = { sessionId ->
                        navController.navigate(AppDestination.ActiveWorkout.createRoute(sessionId))
                    }
                )
            }
            composable(AppDestination.Plans.route) {
                PlansScreen(
                    onOpenPlan = { planId ->
                        navController.navigate(AppDestination.PlanDetail.createRoute(planId))
                    }
                )
            }
            composable(
                route = AppDestination.PlanDetail.route,
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) {
                PlanDetailScreen(
                    onOpenWorkoutEditor = { templateId ->
                        navController.navigate(AppDestination.WorkoutEditor.createRoute(templateId))
                    }
                )
            }
            composable(
                route = AppDestination.WorkoutEditor.route,
                arguments = listOf(navArgument("workoutTemplateId") { type = NavType.LongType })
            ) {
                WorkoutEditorScreen()
            }
            composable(
                route = AppDestination.ActiveWorkout.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStack ->
                val sessionId = backStack.arguments?.getLong("sessionId") ?: 0L
                PlaceholderScreen(text = "Treino em andamento (#$sessionId)")
            }
            composable(AppDestination.Catalog.route) {
                CatalogScreen()
            }
            composable(AppDestination.History.route) {
                PlaceholderScreen(text = stringResource(R.string.placeholder_history))
            }
            composable(AppDestination.Settings.route) {
                PlaceholderScreen(text = stringResource(R.string.placeholder_settings))
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
