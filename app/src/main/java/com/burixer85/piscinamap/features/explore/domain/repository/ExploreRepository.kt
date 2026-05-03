package com.burixer85.piscinamap.features.explore.domain.repository

import com.burixer85.piscinamap.core.domain.model.Pool

interface ExploreRepository {
    suspend fun searchNearbyPools(lat: Double, lng: Double, radius: Int): Result<List<Pool>>
}