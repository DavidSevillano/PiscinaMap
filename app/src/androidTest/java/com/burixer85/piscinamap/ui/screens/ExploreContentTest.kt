package com.burixer85.piscinamap.ui.screens

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.features.explore.presentation.ExploreContent
import com.burixer85.piscinamap.features.explore.presentation.ExploreUiState
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.burixer85.piscinamap.util.TestData
import org.junit.Rule
import org.junit.Test

class ExploreContentTest {

    @get:Rule val rule = createComposeRule()
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun showsLoadingIndicator_whenIsLoadingTrue() {
        rule.setContent {
            PiscinaMapTheme {
                ExploreContent(
                    uiState = ExploreUiState(isLoading = true),
                    onNavigateToDetail = {},
                )
            }
        }
        rule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun showsPoolList_whenPoolsLoaded() {
        rule.setContent {
            PiscinaMapTheme {
                ExploreContent(
                    uiState = ExploreUiState(pools = TestData.poolList),
                    onNavigateToDetail = {},
                )
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").assertIsDisplayed()
        rule.onNodeWithText("Piscina Olímpica Norte").assertIsDisplayed()
    }

    @Test
    fun showsErrorMessage_whenErrorNotNull() {
        rule.setContent {
            PiscinaMapTheme {
                ExploreContent(
                    uiState = ExploreUiState(error = "Network error"),
                    onNavigateToDetail = {},
                )
            }
        }
        rule.onNodeWithText("Network error").assertIsDisplayed()
    }

    @Test
    fun showsEmptyMessage_whenPoolsEmpty() {
        rule.setContent {
            PiscinaMapTheme {
                ExploreContent(
                    uiState = ExploreUiState(pools = emptyList()),
                    onNavigateToDetail = {},
                )
            }
        }
        rule.onNodeWithText(ctx.getString(R.string.no_pools_in_this_area)).assertIsDisplayed()
    }

    @Test
    fun showsExitDialog_whenShowExitConfirmationTrue() {
        rule.setContent {
            PiscinaMapTheme {
                ExploreContent(
                    uiState = ExploreUiState(),
                    onNavigateToDetail = {},
                    showExitConfirmation = true,
                    onConfirmExit = {},
                    onDismissExit = {},
                )
            }
        }
        rule.onNodeWithText(ctx.getString(R.string.exit_app_title)).assertIsDisplayed()
        rule.onNodeWithText(ctx.getString(R.string.exit_app_message)).assertIsDisplayed()
    }
}
