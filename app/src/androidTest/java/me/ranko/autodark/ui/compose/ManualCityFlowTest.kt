package me.ranko.autodark.ui.compose

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.ranko.autodark.Constant.SP_AUTO_TIME_SUNRISE
import me.ranko.autodark.Constant.SP_AUTO_TIME_SUNSET
import me.ranko.autodark.R
import me.ranko.autodark.data.LocationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualCityFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainComposeActivity>()

    @Test
    fun searchSelectScheduleAndRestoreNewYork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = LocationRepository(context)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        composeRule.onNodeWithTag("location_tile")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.city_search_hint))
            .performTextInput("York")
        composeRule.waitUntil(5_000L) {
            composeRule.onAllNodes(hasText("New York City"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("New York City")
            .performClick()

        composeRule.waitUntil(5_000L) {
            repository.getManualCity()?.name == "New York City"
        }
        val selected = repository.getManualCity()
        assertNotNull(selected)
        assertEquals("US", selected?.countryCode)
        assertEquals("America/New_York", selected?.timeZoneId)
        composeRule.onNodeWithText("New York City", substring = true).assertExists()

        val autoToggle = composeRule.onAllNodes(isToggleable())[1]
        val toggleState = autoToggle.fetchSemanticsNode().config[SemanticsProperties.ToggleableState]
        if (toggleState == ToggleableState.Off) {
            autoToggle.performClick()
        }
        composeRule.waitUntil(8_000L) {
            preferences.getBoolean("dark_mode_auto", false) &&
                preferences.getString(SP_AUTO_TIME_SUNRISE, null) != null &&
                preferences.getString(SP_AUTO_TIME_SUNSET, null) != null
        }
        assertTrue(preferences.getBoolean("dark_mode_auto", false))

        composeRule.onNodeWithTag("location_tile")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.restore_automatic_location))
            .performClick()
        composeRule.waitUntil(8_000L) {
            repository.getManualCity() == null
        }
        assertEquals(null, repository.getManualCity())
    }
}
