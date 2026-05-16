package com.burixer85.piscinamap.features.detail.data.repository

import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.dto.*
import com.burixer85.piscinamap.core.data.local.db.PoolDetailCacheDao
import com.burixer85.piscinamap.core.data.local.entity.*
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.detail.domain.repository.DetailRepository
import java.util.Locale
import javax.inject.Inject

class DetailRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi,
    private val poolDetailCacheDao: PoolDetailCacheDao
) : DetailRepository {

    override suspend fun getPoolDetails(placeId: String): Result<Pool> {
        val freshThreshold = System.currentTimeMillis() - DETAIL_TTL_MS
        val cached = poolDetailCacheDao.getDetail(placeId, freshThreshold)
        if (cached != null) return Result.success(cached.toDomain())

        return try {
            val language = Locale.getDefault().language
            val response = api.getPlaceDetails(
                placeId = placeId,
                apiKey = BuildConfig.GOOGLEMAPS_KEY,
                language = language
            )
            val pool = response.result?.toDomain()?.copy(id = placeId)
                ?: return Result.failure(Exception("Pool not found"))

            try {
                poolDetailCacheDao.insertDetail(pool.toDetailCacheEntity())
            } catch (_: Exception) { /* cache write failure is non-fatal */ }
            return Result.success(pool)
        } catch (e: Exception) {
            val stale = poolDetailCacheDao.getDetailIgnoringTtl(placeId)
            if (stale != null) Result.success(stale.toDomain())
            else Result.failure(e)
        }
    }

    companion object {
        private const val DETAIL_TTL_MS = 24 * 60 * 60 * 1000L
    }
}
