package com.burixer85.piscinamap.features.explore.data.repository

import com.burixer85.piscinamap.core.data.PoolSearchDataSource
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.explore.domain.repository.ExploreRepository
import javax.inject.Inject

class ExploreRepositoryImpl @Inject constructor(
    private val poolSearchDataSource: PoolSearchDataSource
) : ExploreRepository {

    override suspend fun searchNearbyPools(lat: Double, lng: Double, radius: Int): Result<List<Pool>> =
        poolSearchDataSource.searchNearbyPools(lat, lng, radius)
}
