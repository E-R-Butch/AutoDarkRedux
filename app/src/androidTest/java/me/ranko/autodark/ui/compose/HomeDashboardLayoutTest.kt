package me.ranko.autodark.ui.compose

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeDashboardLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainComposeActivity>()

    @Test
    fun homeUsesFullWidthHeroAndTwoColumnFeatureMosaic() {
        val heroBounds = composeRule.onNodeWithTag("home_hero")
            .fetchSemanticsNode().boundsInRoot
        val automationBounds = composeRule.onNodeWithTag("automation_panel")
            .performScrollTo()
            .fetchSemanticsNode().boundsInRoot
        val locationBounds = composeRule.onNodeWithTag("location_tile")
            .performScrollTo()
            .fetchSemanticsNode().boundsInRoot
        val wallpaperBounds = composeRule.onNodeWithTag("wallpaper_tile")
            .performScrollTo()
            .fetchSemanticsNode().boundsInRoot

        assertTrue(heroBounds.width > locationBounds.width * 1.75f)
        assertTrue(automationBounds.width > locationBounds.width * 1.75f)
        assertEquals(locationBounds.top, wallpaperBounds.top, 2f)
        assertTrue(locationBounds.right < wallpaperBounds.left)
    }
}
