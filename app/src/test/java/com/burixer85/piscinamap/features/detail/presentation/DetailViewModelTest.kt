package com.burixer85.piscinamap.features.detail.presentation

import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.detail.domain.usecases.GetPoolDetailsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
class DetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var useCase: GetPoolDetailsUseCase
    private lateinit var analytics: AnalyticsManager
    private lateinit var viewModel: DetailViewModel

    private val fakePool = Pool(
        id = "pool1",
        name = "Piscina Test",
        latitude = 40.0,
        longitude = -3.0,
        address = "Calle Test 1",
        rating = 4.5f,
        isOpenNow = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        analytics = mockk(relaxed = true)
        viewModel = DetailViewModel(useCase, analytics)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has isLoading true`() {
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.pool)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `setPoolId triggers load and emits success state`() = runTest {
        coEvery { useCase("pool1") } returns Result.success(fakePool)

        viewModel.setPoolId("pool1")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(fakePool, state.pool)
        assertNull(state.error)
    }

    @Test
    fun `setPoolId triggers load and emits error state on failure`() = runTest {
        coEvery { useCase("pool1") } returns Result.failure(Exception("Network error"))

        viewModel.setPoolId("pool1")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.pool)
        assertEquals("Network error", state.error)
    }

    @Test
    fun `setPoolId with same id does not reload`() = runTest {
        coEvery { useCase("pool1") } returns Result.success(fakePool)

        viewModel.setPoolId("pool1")
        viewModel.setPoolId("pool1")

        coVerify(exactly = 1) { useCase("pool1") }
    }

    @Test
    fun `setPoolId with different id triggers new load`() = runTest {
        val fakePool2 = fakePool.copy(id = "pool2", name = "Piscina 2")
        coEvery { useCase("pool1") } returns Result.success(fakePool)
        coEvery { useCase("pool2") } returns Result.success(fakePool2)

        viewModel.setPoolId("pool1")
        viewModel.setPoolId("pool2")

        assertEquals(fakePool2, viewModel.uiState.value.pool)
    }

    @Test
    fun `retry reloads pool details`() = runTest {
        coEvery { useCase("pool1") } returns Result.success(fakePool)
        viewModel.setPoolId("pool1")

        val updatedPool = fakePool.copy(name = "Piscina Actualizada")
        coEvery { useCase("pool1") } returns Result.success(updatedPool)
        viewModel.retry()

        assertEquals("Piscina Actualizada", viewModel.uiState.value.pool?.name)
    }

    @Test
    fun `retry after error clears error and shows pool on success`() = runTest {
        coEvery { useCase("pool1") } returns Result.failure(Exception("Error"))
        viewModel.setPoolId("pool1")

        coEvery { useCase("pool1") } returns Result.success(fakePool)
        viewModel.retry()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(fakePool, state.pool)
    }

    @Test
    fun `loadPoolDetails tracks DetailScreen and pool_detail_viewed on success`() = runTest {
        coEvery { useCase("pool1") } returns Result.success(fakePool)

        viewModel.setPoolId("pool1")

        verify { analytics.trackScreen("DetailScreen") }
        verify {
            analytics.trackEvent(
                "pool_detail_viewed",
                mapOf("pool_id" to "pool1", "pool_name" to "Piscina Test")
            )
        }
    }

    @Test
    fun `loadPoolDetails logs non-fatal error on failure`() = runTest {
        val exception = Exception("Network error")
        coEvery { useCase("pool1") } returns Result.failure(exception)

        viewModel.setPoolId("pool1")

        verify { analytics.logNonFatalError(exception) }
    }

    @Test
    fun `onFavoriteToggled tracks favorite_added when isFavorite is true`() {
        viewModel.onFavoriteToggled("pool1", true)

        verify { analytics.trackEvent("favorite_added", mapOf("pool_id" to "pool1")) }
    }

    @Test
    fun `onFavoriteToggled tracks favorite_removed when isFavorite is false`() {
        viewModel.onFavoriteToggled("pool1", false)

        verify { analytics.trackEvent("favorite_removed", mapOf("pool_id" to "pool1")) }
    }

    @Test
    fun `onPoolHidden tracks pool_hidden event`() {
        viewModel.onPoolHidden("pool1")

        verify { analytics.trackEvent("pool_hidden", mapOf("pool_id" to "pool1")) }
    }

    @Test
    fun `onPoolUnhidden tracks pool_unhidden event`() {
        viewModel.onPoolUnhidden("pool1")

        verify { analytics.trackEvent("pool_unhidden", mapOf("pool_id" to "pool1")) }
    }
}
