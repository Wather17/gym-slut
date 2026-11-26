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
}
