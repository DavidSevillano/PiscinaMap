package com.burixer85.piscinamap.home.data.repository

import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.dto.toDomain
import com.burixer85.piscinamap.home.domain.repository.PoolRepository
import javax.inject.Inject
import com.burixer85.piscinamap.core.domain.model.Pool

class PoolRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi
) : PoolRepository {
    override suspend fun searchNearbyPools(lat: Double, lng: Double, radius: Int): Result<List<Pool>> {
        return try {
            val response = api.getNearbyPools("$lat,$lng", radius, apiKey = BuildConfig.GOOGLEMAPS_KEY)
            val pools = response.results.map { it.toDomain() }
            Result.success(pools)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPoolDetails(placeId: String): Result<Pool> {
        return TODO("Provide the return value")
    }
}