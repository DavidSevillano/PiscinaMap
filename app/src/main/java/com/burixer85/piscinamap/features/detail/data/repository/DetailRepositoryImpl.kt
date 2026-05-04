package com.burixer85.piscinamap.features.detail.data.repository

import android.util.Log
import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.dto.toDomain
import com.burixer85.piscinamap.features.detail.domain.repository.DetailRepository
import com.burixer85.piscinamap.core.domain.model.Pool
import java.util.Locale
import javax.inject.Inject

class DetailRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi
) : DetailRepository {
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