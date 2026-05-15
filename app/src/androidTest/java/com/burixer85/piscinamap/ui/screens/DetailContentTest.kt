package com.burixer85.piscinamap.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.features.detail.presentation.DetailContent
import com.burixer85.piscinamap.features.detail.presentation.DetailUiState
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.burixer85.piscinamap.util.TestData
import org.junit.Rule
import org.junit.Test

class DetailContentTest {

    @get:Rule val rule = createComposeRule()
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun showsLoadingState_whenPoolAndErrorAreNull() {
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(uiState = DetailUiState(isLoading = true, pool = null, error = null))
            }
        }
        rule.onAllNodesWithText(ctx.getString(R.string.retry)).assertCountEquals(0)
        rule.onAllNodesWithText("Piscina Municipal Centro").assertCountEquals(0)
    }

    @Test
    fun showsPoolName_whenPoolLoaded() {
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(uiState = DetailUiState(pool = TestData.pool))
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").assertIsDisplayed()
    }

    @Test
    fun showsPoolAddress_whenPoolLoaded() {
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(uiState = DetailUiState(pool = TestData.pool))
            }
        }
        rule.onNodeWithText("Calle Mayor 1, Madrid").assertIsDisplayed()
    }

    @Test
    fun showsErrorAndRetryButton_whenErrorNotNull() {
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(uiState = DetailUiState(error = "Connection failed"))
            }
        }
        rule.onNodeWithText("Error: Connection failed").assertIsDisplayed()
        rule.onNodeWithText(ctx.getString(R.string.retry)).assertIsDisplayed()
    }

    @Test
    fun invokesRetry_whenRetryButtonClicked() {
        var retryCalled = false
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(
                    uiState = DetailUiState(error = "Connection failed"),
                    onRetry = { retryCalled = true },
                )
            }
        }
        rule.onNodeWithText(ctx.getString(R.string.retry)).performClick()
        assert(retryCalled)
    }

    @Test
    fun showsHiddenMessage_whenPoolIsHidden() {
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(
                    uiState = DetailUiState(pool = TestData.pool),
                    isHidden = true,
                )
            }
        }
        rule.onNodeWithText(ctx.getString(R.string.pool_hidden)).assertIsDisplayed()
    }

    @Test
    fun showsHideOption_inDropdownMenu_whenMenuExpanded() {
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(
                    uiState = DetailUiState(pool = TestData.pool),
                    isHidden = false,
                    showMenu = true,
                )
            }
        }
        rule.onNodeWithText(ctx.getString(R.string.not_a_pool)).assertIsDisplayed()
    }

    @Test
    fun showsUnhideOption_inDropdownMenu_whenPoolIsHiddenAndMenuExpanded() {
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(
                    uiState = DetailUiState(pool = TestData.pool),
                    isHidden = true,
                    showMenu = true,
                )
            }
        }
        rule.onNodeWithText(ctx.getString(R.string.unhide_pool)).assertIsDisplayed()
    }

    @Test
    fun showsFavoriteBorderIcon_whenNotFavorited() {
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(
                    uiState = DetailUiState(pool = TestData.pool),
                    isFavorite = false,
                )
            }
        }
        rule.onNodeWithContentDescription(ctx.getString(R.string.add_to_favorites)).assertIsDisplayed()
    }

    @Test
    fun showsFilledFavoriteIcon_whenFavorited() {
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(
                    uiState = DetailUiState(pool = TestData.pool),
                    isFavorite = true,
                )
            }
        }
        rule.onNodeWithContentDescription(ctx.getString(R.string.remove_from_favorites)).assertIsDisplayed()
    }

    @Test
    fun invokesFavoriteToggle_whenHeartIconClicked() {
        var toggled = false
        rule.setContent {
            PiscinaMapTheme {
                DetailContent(
                    uiState = DetailUiState(pool = TestData.pool),
                    isFavorite = false,
                    onFavoriteToggle = { toggled = true },
                )
            }
        }
        rule.onNodeWithContentDescription(ctx.getString(R.string.add_to_favorites)).performClick()
        assert(toggled)
    }
}
