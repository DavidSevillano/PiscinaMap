package com.burixer85.piscinamap.features.home.domain.repository

import com.burixer85.piscinamap.core.domain.model.Pool

interface PoolRepository {
    suspend fun searchNearbyPools(lat: Double, lng: Double, radius: Int): Result<List<Pool>>
    suspend fun getPoolDetails(placeId: String): Result<Pool>
}