package com.burixer85.piscinamap.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.features.favorites.presentation.FavoritesContent
import com.burixer85.piscinamap.features.favorites.presentation.FavoritesUiState
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.burixer85.piscinamap.util.TestData
import org.junit.Rule
import org.junit.Test

class FavoritesContentTest {

    @get:Rule val rule = createComposeRule()
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun showsEmptyMessage_whenNoFavorites() {
        rule.setContent {
            PiscinaMapTheme {
                FavoritesContent(uiState = FavoritesUiState(isEmpty = true))
            }
        }
        rule.onNodeWithText(ctx.getString(R.string.no_favorites)).assertIsDisplayed()
    }

    @Test
    fun showsPoolName_whenFavoritesLoaded() {
        rule.setContent {
            PiscinaMapTheme {
                FavoritesContent(
                    uiState = FavoritesUiState(pools = listOf(TestData.pool)),
                )
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").assertIsDisplayed()
    }

    @Test
    fun tapPoolCard_triggersOnNavigateToDetail_withCorrectId() {
        var clickedId: String? = null
        rule.setContent {
            PiscinaMapTheme {
                FavoritesContent(
                    uiState = FavoritesUiState(pools = listOf(TestData.pool)),
                    onNavigateToDetail = { id -> clickedId = id },
                )
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").performClick()
        assert(clickedId == TestData.pool.id)
    }
}
