package com.burixer85.piscinamap.features.explore.presentation

import com.burixer85.piscinamap.core.domain.model.FilterState
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.domain.model.PoolType
import com.burixer85.piscinamap.features.explore.domain.usecases.GetExploreNearbyPoolsUseCase
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelFilterTest {

    private val mockUseCase = mockk<GetExploreNearbyPoolsUseCase>(relaxed = true)
    private lateinit var viewModel: ExploreViewModel

    private fun makePool(
        id: String,
        rating: Float? = null,
        isOpenNow: Boolean? = null,
        lat: Double = 40.4168,
        lng: Double = -3.7038,
        poolType: PoolType = PoolType.UNKNOWN
    ) = Pool(
        id = id,
        name = "Pool $id",
        latitude = lat,
        longitude = lng,
        address = "",
        rating = rating,
        isOpenNow = isOpenNow,
        poolType = poolType
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = ExploreViewModel(mockUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `applyFilters returns all pools when FilterState is default`() {
        val pools = listOf(makePool("1"), makePool("2"), makePool("3"))
        val result = viewModel.applyFilters(pools, FilterState(), null)
        assertEquals(pools, result)
    }

    @Test
    fun `applyFilters filters by minRating`() {
        val pools = listOf(
            makePool("1", rating = 4.5f),
            makePool("2", rating = 3.5f),
            makePool("3", rating = null)
        )
        val result = viewModel.applyFilters(pools, FilterState(minRating = 4f), null)
        assertEquals(listOf(pools[0]), result)
    }

    @Test
    fun `applyFilters openNow excludes pools with isOpenNow null or false`() {
        val pools = listOf(
            makePool("1", isOpenNow = true),
            makePool("2", isOpenNow = false),
            makePool("3", isOpenNow = null)
        )
        val result = viewModel.applyFilters(pools, FilterState(openNow = true), null)
        assertEquals(listOf(pools[0]), result)
    }

    @Test
    fun `applyFilters openNow false does not filter`() {
        val pools = listOf(makePool("1", isOpenNow = true), makePool("2", isOpenNow = false))
        val result = viewModel.applyFilters(pools, FilterState(openNow = false), null)
        assertEquals(pools, result)
    }

    @Test
    fun `applyFilters filters by maxDistanceKm`() {
        val nearPool = makePool("near", lat = 40.46, lng = -3.70)
        val farPool = makePool("far", lat = 40.68, lng = -3.70)
        val userLatLng = 40.4168 to -3.7038

        val result = viewModel.applyFilters(
            listOf(nearPool, farPool),
            FilterState(maxDistanceKm = 10),
            userLatLng
        )
        assertEquals(listOf(nearPool), result)
    }

    @Test
    fun `applyFilters distance returns all when userLatLng is null`() {
        val pools = listOf(makePool("1", lat = 60.0, lng = 20.0), makePool("2", lat = 0.0, lng = 0.0))
        val result = viewModel.applyFilters(pools, FilterState(maxDistanceKm = 1), null)
        assertEquals(pools, result)
    }

    @Test
    fun `applyFilters filters by single type`() {
        val pools = listOf(
            makePool("1", poolType = PoolType.HOTEL),
            makePool("2", poolType = PoolType.PUBLIC),
            makePool("3", poolType = PoolType.MUNICIPAL)
        )
        val result = viewModel.applyFilters(
            pools,
            FilterState(selectedTypes = setOf(PoolType.HOTEL)),
            null
        )
        assertEquals(listOf(pools[0]), result)
    }

    @Test
    fun `applyFilters filters by multiple types`() {
        val pools = listOf(
            makePool("1", poolType = PoolType.HOTEL),
            makePool("2", poolType = PoolType.PUBLIC),
            makePool("3", poolType = PoolType.MUNICIPAL)
        )
        val result = viewModel.applyFilters(
            pools,
            FilterState(selectedTypes = setOf(PoolType.HOTEL, PoolType.PUBLIC)),
            null
        )
        assertEquals(listOf(pools[0], pools[1]), result)
    }

    @Test
    fun `applyFilters empty selectedTypes does not filter by type`() {
        val pools = listOf(makePool("1", poolType = PoolType.HOTEL), makePool("2", poolType = PoolType.UNKNOWN))
        val result = viewModel.applyFilters(pools, FilterState(selectedTypes = emptySet()), null)
        assertEquals(pools, result)
    }

    @Test
    fun `applyFilters combines multiple active filters`() {
        val pools = listOf(
            makePool("1", rating = 4.5f, isOpenNow = true, poolType = PoolType.HOTEL),
            makePool("2", rating = 4.5f, isOpenNow = false, poolType = PoolType.HOTEL),
            makePool("3", rating = 3.0f, isOpenNow = true, poolType = PoolType.HOTEL)
        )
        val result = viewModel.applyFilters(
            pools,
            FilterState(minRating = 4f, openNow = true),
            null
        )
        assertEquals(listOf(pools[0]), result)
    }

    @Test
    fun `haversineKm returns approximately correct distance`() {
        val distance = viewModel.haversineKm(40.4168, -3.7038, 41.3851, 2.1734)
        assertTrue("Expected ~504km, got $distance", distance in 500.0..510.0)
    }
}
