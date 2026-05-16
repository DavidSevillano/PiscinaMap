package com.burixer85.piscinamap.features.explore.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.burixer85.piscinamap.core.domain.model.FilterState
import com.burixer85.piscinamap.core.domain.model.PoolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterChipsRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chipOpenNow_togglesOnOff() {
        var emitted = FilterState()
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(), onFiltersChange = { emitted = it })
        }
        composeRule.onNodeWithText("Abierto ahora").performClick()
        assertTrue(emitted.openNow)
    }

    @Test
    fun chipRating_cyclesThrough3f_4f_45f_null() {
        val states = mutableListOf<FilterState>()
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(), onFiltersChange = { states.add(it) })
        }
        composeRule.onNodeWithText("Valoración").performClick()
        assertEquals(3f, states.last().minRating)
    }

    @Test
    fun chipRating_activeChip_showsLabel() {
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(minRating = 4f), onFiltersChange = {})
        }
        composeRule.onNodeWithText("4+").assertIsDisplayed()
    }

    @Test
    fun chipDistance_cyclesThrough5_10_25_null() {
        val states = mutableListOf<FilterState>()
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(), onFiltersChange = { states.add(it) })
        }
        composeRule.onNodeWithText("Distancia").performClick()
        assertEquals(5, states.last().maxDistanceKm)
    }

    @Test
    fun chipDistance_activeChip_showsKmLabel() {
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(maxDistanceKm = 10), onFiltersChange = {})
        }
        composeRule.onNodeWithText("< 10km").assertIsDisplayed()
    }

    @Test
    fun chipTipo_opensDropdown() {
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(), onFiltersChange = {})
        }
        composeRule.onNodeWithText("Tipo").performClick()
        composeRule.onNodeWithText("Pública").assertIsDisplayed()
        composeRule.onNodeWithText("Municipal").assertIsDisplayed()
        composeRule.onNodeWithText("Hotel").assertIsDisplayed()
    }

    @Test
    fun chipTipo_selectingHotel_emitsCorrectState() {
        var emitted = FilterState()
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(), onFiltersChange = { emitted = it })
        }
        composeRule.onNodeWithText("Tipo").performClick()
        composeRule.onNodeWithText("Hotel").performClick()
        assertTrue(PoolType.HOTEL in emitted.selectedTypes)
    }

    @Test
    fun chipOpenNow_whenActive_showsAbiertoLabel_andClearsOnClick() {
        var emitted = FilterState(openNow = true)
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(openNow = true), onFiltersChange = { emitted = it })
        }
        composeRule.onNodeWithText("Abierto").assertIsDisplayed()
        composeRule.onNodeWithText("Abierto").performClick()
        assertTrue(!emitted.openNow)
    }

    @Test
    fun chipRating_fullCycle_nullTo3fTo4fTo45fToNull() {
        val states = mutableListOf<FilterState>()
        composeRule.setContent {
            var currentFilters by remember { mutableStateOf(FilterState()) }
            FilterChipsRow(
                filters = currentFilters,
                onFiltersChange = {
                    currentFilters = it
                    states.add(it)
                }
            )
        }
        // null -> 3f
        composeRule.onNodeWithText("Valoración").performClick()
        assertEquals(3f, states.last().minRating)
        // 3f -> 4f
        composeRule.onNodeWithText("3+").performClick()
        assertEquals(4f, states.last().minRating)
        // 4f -> 4.5f
        composeRule.onNodeWithText("4+").performClick()
        assertEquals(4.5f, states.last().minRating)
        // 4.5f -> null
        composeRule.onNodeWithText("4.5+").performClick()
        assertNull(states.last().minRating)
    }

    @Test
    fun chipDistance_fullCycle_nullTo5To10To25ToNull() {
        val states = mutableListOf<FilterState>()
        composeRule.setContent {
            var currentFilters by remember { mutableStateOf(FilterState()) }
            FilterChipsRow(
                filters = currentFilters,
                onFiltersChange = {
                    currentFilters = it
                    states.add(it)
                }
            )
        }
        // null -> 5
        composeRule.onNodeWithText("Distancia").performClick()
        assertEquals(5, states.last().maxDistanceKm)
        // 5 -> 10
        composeRule.onNodeWithText("< 5km").performClick()
        assertEquals(10, states.last().maxDistanceKm)
        // 10 -> 25
        composeRule.onNodeWithText("< 10km").performClick()
        assertEquals(25, states.last().maxDistanceKm)
        // 25 -> null
        composeRule.onNodeWithText("< 25km").performClick()
        assertNull(states.last().maxDistanceKm)
    }

    @Test
    fun chipTipo_multipleSelections_storesBothTypes() {
        var emitted = FilterState()
        composeRule.setContent {
            var currentFilters by remember { mutableStateOf(FilterState()) }
            FilterChipsRow(
                filters = currentFilters,
                onFiltersChange = {
                    currentFilters = it
                    emitted = it
                }
            )
        }
        // Select Hotel
        composeRule.onNodeWithText("Tipo").performClick()
        composeRule.onNodeWithText("Hotel").performClick()
        // Select Municipal (dropdown should still be open)
        composeRule.onNodeWithText("Municipal").performClick()
        assertTrue(PoolType.HOTEL in emitted.selectedTypes)
        assertTrue(PoolType.MUNICIPAL in emitted.selectedTypes)
    }
}
