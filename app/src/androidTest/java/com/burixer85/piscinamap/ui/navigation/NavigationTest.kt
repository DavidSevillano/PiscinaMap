package com.burixer85.piscinamap.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.burixer85.piscinamap.features.explore.presentation.ExploreContent
import com.burixer85.piscinamap.features.explore.presentation.ExploreUiState
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.burixer85.piscinamap.util.TestData
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun exploreContent_invokesNavigateToDetail_whenPoolCardTapped() {
        var navigatedToId = ""
        rule.setContent {
            PiscinaMapTheme {
                ExploreContent(
                    uiState = ExploreUiState(pools = TestData.poolList),
                    onFetchMore = {},
                    onNavigateToDetail = { navigatedToId = it },
                )
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").performClick()
        assert(navigatedToId == "test_pool_id_1")
    }
}
