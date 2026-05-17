package com.burixer85.piscinamap.features.explore.presentation

import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.explore.domain.usecases.GetExploreNearbyPoolsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var useCase: GetExploreNearbyPoolsUseCase
    private lateinit var analytics: AnalyticsManager
    private lateinit var viewModel: ExploreViewModel

    private fun makePool(id: String) = Pool(
        id = id,
        name = "Pool $id",
        latitude = 40.0,
        longitude = -3.0,
        address = "Address $id",
        rating = null,
        isOpenNow = null
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        analytics = mockk(relaxed = true)
        viewModel = ExploreViewModel(useCase, analytics)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has isLoading true and empty pools`() {
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.pools.isEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `fetchPools success updates pools and clears loading`() = runTest {
        val pools = listOf(makePool("1"), makePool("2"))
        coEvery { useCase(any(), any(), any()) } returns Result.success(pools)

        viewModel.fetchPools(40.0, -3.0)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(pools, state.pools)
        assertNull(state.error)
    }

    @Test
    fun `fetchPools failure emits error and clears loading`() = runTest {
        coEvery { useCase(any(), any(), any()) } returns Result.failure(Exception("Timeout"))

        viewModel.fetchPools(40.0, -3.0)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Timeout", state.error)
        assertTrue(state.pools.isEmpty())
    }

    @Test
    fun `fetchPools tracks ExploreScreen`() = runTest {
        coEvery { useCase(any(), any(), any()) } returns Result.success(emptyList())

        viewModel.fetchPools(40.0, -3.0)

        verify { analytics.trackScreen("ExploreScreen") }
    }

    @Test
    fun `fetchPools tracks map_area_searched with coordinates`() = runTest {
        coEvery { useCase(any(), any(), any()) } returns Result.success(emptyList())

        viewModel.fetchPools(40.4168, -3.7038)

        verify {
            analytics.trackEvent(
                "map_area_searched",
                mapOf("latitude" to "40.4168", "longitude" to "-3.7038")
            )
        }
    }

    @Test
    fun `fetchPools logs non-fatal error on failure`() = runTest {
        val exception = Exception("timeout")
        coEvery { useCase(any(), any(), any()) } returns Result.failure(exception)

        viewModel.fetchPools(40.0, -3.0)

        verify { analytics.logNonFatalError(exception) }
    }
}
