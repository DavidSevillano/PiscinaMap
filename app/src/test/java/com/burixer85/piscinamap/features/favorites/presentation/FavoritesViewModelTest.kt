package com.burixer85.piscinamap.features.favorites.presentation

import android.content.Context
import android.content.SharedPreferences
import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
import com.burixer85.piscinamap.core.presentation.util.PoolStateManager
import com.burixer85.piscinamap.features.detail.domain.repository.DetailRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var detailRepository: DetailRepository
    private lateinit var analytics: AnalyticsManager
    private val backingSet = mutableSetOf<String>()

    private val fakePool = Pool(
        id = "test_pool_id_1",
        name = "Piscina Municipal Centro",
        latitude = 40.4168,
        longitude = -3.7038,
        address = "Calle Mayor 1, Madrid",
        rating = 4.2f,
        isOpenNow = true,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        backingSet.clear()

        mockkObject(PoolStateManager)
        every { PoolStateManager.subscribeFavorite(any()) } returns Unit
        every { PoolStateManager.unsubscribeFavorite(any()) } returns Unit


        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putStringSet(any(), any()) } answers {
            backingSet.clear()
            backingSet.addAll(secondArg<Set<String>>())
            editor
        }
        every { editor.apply() } returns Unit

        prefs = mockk {
            every { getStringSet(any(), any()) } answers { backingSet.toSet() }
            every { edit() } returns editor
        }

        context = mockk {
            every { getSharedPreferences("favorite_pools", Context.MODE_PRIVATE) } returns prefs
        }

        detailRepository = mockk()
        analytics = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(PoolStateManager)
    }

    @Test
    fun `isEmpty is true when no favorites`() = runTest {
        val vm = FavoritesViewModel(context, detailRepository, analytics)
        advanceUntilIdle()
        assert(vm.uiState.value.isEmpty)
        assert(vm.uiState.value.pools.isEmpty())
    }

    @Test
    fun `pools populated from saved IDs`() = runTest {
        backingSet.add(fakePool.id)
        coEvery { detailRepository.getPoolDetails(fakePool.id) } returns Result.success(fakePool)

        val vm = FavoritesViewModel(context, detailRepository, analytics)
        advanceUntilIdle()

        assert(!vm.uiState.value.isEmpty)
        assert(vm.uiState.value.pools.size == 1)
        assert(vm.uiState.value.pools[0].id == fakePool.id)
    }

    @Test
    fun `updateFavoriteState removes pool when unfavorited`() = runTest {
        backingSet.add(fakePool.id)
        coEvery { detailRepository.getPoolDetails(fakePool.id) } returns Result.success(fakePool)

        val vm = FavoritesViewModel(context, detailRepository, analytics)
        advanceUntilIdle()
        assert(vm.uiState.value.pools.size == 1)

        vm.updateFavoriteState(fakePool.id, false)
        advanceUntilIdle()

        assert(vm.uiState.value.pools.isEmpty())
        assert(vm.uiState.value.isEmpty)
    }

    @Test
    fun `updateFavoriteState adds pool when favorited externally`() = runTest {
        val vm = FavoritesViewModel(context, detailRepository, analytics)
        advanceUntilIdle()
        assert(vm.uiState.value.isEmpty)

        coEvery { detailRepository.getPoolDetails(fakePool.id) } returns Result.success(fakePool)
        vm.updateFavoriteState(fakePool.id, true)
        advanceUntilIdle()

        assert(!vm.uiState.value.isEmpty)
        assert(vm.uiState.value.pools.size == 1)
    }

    @Test
    fun `init tracks FavoritesScreen`() = runTest {
        val vm = FavoritesViewModel(context, detailRepository, analytics)
        advanceUntilIdle()

        verify { analytics.trackScreen("FavoritesScreen") }
    }
}
