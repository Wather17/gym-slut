package com.gymslut.app.core.navigation

import com.gymslut.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationTest {

    @Test
    fun bottomNavTabs_containsExactlyFourTabsInExpectedOrder() {
        assertEquals(4, BottomNavTabs.size)
        assertEquals(
            listOf(
                AppDestination.Today,
                AppDestination.Plans,
                AppDestination.Catalog,
                AppDestination.History
            ),
            BottomNavTabs.map { it.destination }
        )
        assertEquals(
            listOf(
                R.string.nav_today,
                R.string.nav_plans,
                R.string.nav_catalog,
                R.string.nav_history
            ),
            BottomNavTabs.map { it.labelRes }
        )
    }

    @Test
    fun secondaryDestinations_createExpectedRoutes() {
        assertEquals("settings", AppDestination.Settings.route)
        assertEquals("plan_detail/42", AppDestination.PlanDetail.createRoute(42L))
        assertEquals("workout_editor/7", AppDestination.WorkoutEditor.createRoute(7L))
        assertEquals("active_workout/99", AppDestination.ActiveWorkout.createRoute(99L))
        assertEquals("session_detail/15", AppDestination.SessionDetail.createRoute(15L))
        assertEquals("exercise_history/8", AppDestination.ExerciseHistory.createRoute(8L))
    }
}
