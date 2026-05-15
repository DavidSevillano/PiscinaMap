package com.burixer85.piscinamap.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.presentation.components.PoolListCard
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.burixer85.piscinamap.util.TestData
import org.junit.Rule
import org.junit.Test

class PoolListCardTest {

    @get:Rule val rule = createComposeRule()
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun showsPoolName() {
        rule.setContent {
            PiscinaMapTheme {
                PoolListCard(pool = TestData.pool, onNavigateToDetail = {})
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").assertIsDisplayed()
    }

    @Test
    fun showsRating() {
        rule.setContent {
            PiscinaMapTheme {
                PoolListCard(pool = TestData.pool, onNavigateToDetail = {})
            }
        }
        rule.onNodeWithText("4.2").assertIsDisplayed()
    }

    @Test
    fun showsNaRating_whenRatingIsNull() {
        val poolNoRating = TestData.pool.copy(rating = null)
        rule.setContent {
            PiscinaMapTheme {
                PoolListCard(pool = poolNoRating, onNavigateToDetail = {})
            }
        }
        rule.onNodeWithText("N/A").assertIsDisplayed()
    }

    @Test
    fun showsOpenStatus_whenPoolIsOpen() {
        rule.setContent {
            PiscinaMapTheme {
                PoolListCard(pool = TestData.pool.copy(isOpenNow = true), onNavigateToDetail = {})
            }
        }
        rule.onNodeWithText(ctx.getString(R.string.open)).assertIsDisplayed()
    }

    @Test
    fun invokesNavigationCallback_whenClicked() {
        var navigatedId = ""
        rule.setContent {
            PiscinaMapTheme {
                PoolListCard(pool = TestData.pool, onNavigateToDetail = { navigatedId = it })
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").performClick()
        assert(navigatedId == "test_pool_id_1")
    }
}
