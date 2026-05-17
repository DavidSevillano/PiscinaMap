package com.burixer85.piscinamap.features.home.data.repository

import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.PoolSearchDataSource
import com.burixer85.piscinamap.core.data.dto.toDomain
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import java.util.Locale
import javax.inject.Inject

class PoolRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi,
    private val poolSearchDataSource: PoolSearchDataSource
) : PoolRepository {

    override suspend fun searchNearbyPools(lat: Double, lng: Double, radius: Int): Result<List<Pool>> =
        poolSearchDataSource.searchNearbyPools(lat, lng, radius)

    override suspend fun getPoolDetails(placeId: String): Result<Pool> {
        return try {
            val language = Locale.getDefault().language
            val response = api.getPlaceDetails(
                placeId = placeId,
                apiKey = BuildConfig.GOOGLEMAPS_KEY,
                language = language
            )
            val pool = response.result?.toDomain()?.copy(id = placeId)
                ?: return Result.failure(Exception("Pool not found"))
            Result.success(pool)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
