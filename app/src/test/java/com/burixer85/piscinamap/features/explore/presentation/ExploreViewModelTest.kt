package com.burixer85.piscinamap.features.explore.presentation

import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.explore.domain.usecases.GetExploreNearbyPoolsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var useCase: GetExploreNearbyPoolsUseCase
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
        viewModel = ExploreViewModel(useCase)
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
        assertNull(state.warning)
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
    fun `fetchPools resets hasSearchedMore`() = runTest {
        coEvery { useCase(any(), any(), any()) } returns Result.success(listOf(makePool("1")))

        viewModel.fetchPools(40.0, -3.0)

        assertFalse(viewModel.uiState.value.hasSearchedMore)
    }

    @Test
    fun `fetchMorePools appends only unique pools`() = runTest {
        val initial = listOf(makePool("1"), makePool("2"))
        val more = listOf(makePool("2"), makePool("3"))
        coEvery { useCase(any(), any(), any()) } returnsMany listOf(
            Result.success(initial),
            Result.success(more)
        )

        viewModel.fetchPools(40.0, -3.0)
        viewModel.fetchMorePools()

        val state = viewModel.uiState.value
        assertEquals(3, state.pools.size)
        assertEquals(listOf("1", "2", "3"), state.pools.map { it.id })
        assertNull(state.warning)
    }

    @Test
    fun `fetchMorePools shows warning when no new pools found`() = runTest {
        val initial = listOf(makePool("1"))
        coEvery { useCase(any(), any(), any()) } returnsMany listOf(
            Result.success(initial),
            Result.success(initial)
        )

        viewModel.fetchPools(40.0, -3.0)
        viewModel.fetchMorePools()

        val state = viewModel.uiState.value
        assertNotNull(state.warning)
        assertEquals(1, state.pools.size)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `fetchMorePools is skipped when hasSearchedMore is true`() = runTest {
        val initial = listOf(makePool("1"))
        val more = listOf(makePool("2"))
        coEvery { useCase(any(), any(), any()) } returnsMany listOf(
            Result.success(initial),
            Result.success(more),
            Result.success(listOf(makePool("3")))
        )

        viewModel.fetchPools(40.0, -3.0)
        viewModel.fetchMorePools()
        viewModel.fetchMorePools()  // should be a no-op

        assertEquals(2, viewModel.uiState.value.pools.size)
    }

    @Test
    fun `fetchMorePools failure emits error and clears isLoadingMore`() = runTest {
        val initial = listOf(makePool("1"))
        coEvery { useCase(any(), any(), any()) } returnsMany listOf(
            Result.success(initial),
            Result.failure(Exception("Load more failed"))
        )

        viewModel.fetchPools(40.0, -3.0)
        viewModel.fetchMorePools()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingMore)
        assertEquals("Load more failed", state.error)
    }
}
