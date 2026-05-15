package com.burixer85.piscinamap.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.presentation.components.PoolDetailCard
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.burixer85.piscinamap.util.TestData
import org.junit.Rule
import org.junit.Test

class PoolDetailCardTest {

    @get:Rule val rule = createComposeRule()
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setCard(
        onClose: () -> Unit = {},
        onNavigateToDetail: (String) -> Unit = {},
    ) {
        rule.setContent {
            PiscinaMapTheme {
                PoolDetailCard(
                    pool = TestData.pool,
                    onClose = onClose,
                    onNavigateToDetail = onNavigateToDetail,
                )
            }
        }
    }

    @Test
    fun showsPoolName() {
        setCard()
        rule.onNodeWithText("Piscina Municipal Centro").assertIsDisplayed()
    }

    @Test
    fun showsRating() {
        setCard()
        rule.onNodeWithText("4.2").assertIsDisplayed()
    }

    @Test
    fun showsAddress() {
        setCard()
        rule.onNodeWithText("Calle Mayor 1, Madrid").assertIsDisplayed()
    }

    @Test
    fun showsGetDirectionsButton() {
        setCard()
        rule.onNodeWithText(ctx.getString(R.string.get_directions)).assertIsDisplayed()
    }

    @Test
    fun invokesNavigateCallback_whenCardTapped() {
        // The whole card column is clickable; "Get Directions" opens Google Maps directly.
        var navigatedId = ""
        setCard(onNavigateToDetail = { navigatedId = it })
        rule.onNodeWithText("Piscina Municipal Centro").performClick()
        assert(navigatedId == "test_pool_id_1")
    }
}
