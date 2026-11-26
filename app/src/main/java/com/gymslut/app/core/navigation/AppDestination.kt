package com.gymslut.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.ui.graphics.vector.ImageVector
import com.gymslut.app.R

/**
 * Destinos de navegação principais e secundários do Gym Slut.
 */
sealed class AppDestination(val route: String) {
    data object Today : AppDestination("today")
    data object Plans : AppDestination("plans")
    data object Catalog : AppDestination("catalog")
    data object History : AppDestination("history")
    data object Settings : AppDestination("settings")
    data object PlanDetail : AppDestination("plan_detail/{planId}") {
        fun createRoute(planId: Long): String = "plan_detail/$planId"
    }
    data object WorkoutEditor : AppDestination("workout_editor/{workoutTemplateId}") {
        fun createRoute(workoutTemplateId: Long): String = "workout_editor/$workoutTemplateId"
    }
}

/**
 * Representa uma aba fixa da barra de navegação inferior.
 */
data class BottomNavTab(
    val destination: AppDestination,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

val BottomNavTabs: List<BottomNavTab> = listOf(
    BottomNavTab(
        destination = AppDestination.Today,
        labelRes = R.string.nav_today,
        icon = Icons.Outlined.CalendarToday
    ),
    BottomNavTab(
        destination = AppDestination.Plans,
        labelRes = R.string.nav_plans,
        icon = Icons.AutoMirrored.Outlined.ListAlt
    ),
    BottomNavTab(
        destination = AppDestination.Catalog,
        labelRes = R.string.nav_catalog,
        icon = Icons.Outlined.FitnessCenter
    ),
    BottomNavTab(
        destination = AppDestination.History,
        labelRes = R.string.nav_history,
        icon = Icons.Outlined.History
    )
)
