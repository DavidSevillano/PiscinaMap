package com.burixer85.piscinamap.features.home.data.repository

import android.content.Context
import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.dto.*
import com.burixer85.piscinamap.core.data.local.db.PoolCacheDao
import com.burixer85.piscinamap.core.data.local.entity.*
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
import com.burixer85.piscinamap.core.presentation.util.HiddenPoolsManager
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import java.util.Locale
import javax.inject.Inject
import kotlin.math.cos

class PoolRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi,
    private val context: Context,
    private val poolCacheDao: PoolCacheDao
) : PoolRepository {

    override suspend fun searchNearbyPools(lat: Double, lng: Double, radius: Int): Result<List<Pool>> {
        return try {
            val language = Locale.getDefault().language
            val response = api.getNearbyPools(
                location = "$lat,$lng",
                radius = radius,
                keyword = "swimming_pool",
                language = language,
                apiKey = BuildConfig.GOOGLEMAPS_KEY
            )

            when (response.status) {
                "REQUEST_DENIED" -> return Result.failure(Exception("Error de API: Solicitud denegada"))
                "INVALID_REQUEST" -> return Result.failure(Exception("Solicitud inválida"))
                "OVER_QUERY_LIMIT" -> throw Exception("Cuota de API excedida")
                "UNKNOWN_ERROR" -> throw Exception("Error del servidor")
                "ZERO_RESULTS", "OK" -> {}
                else -> {}
            }

            val pools = response.results
                .filter { place ->
                    val name = place.name.lowercase()
                    val excludePatterns = listOf("piscinas", "piscina s", "piscinas triana", "acuaeuropa", "piscinas sevilla", "piscina sevilla", "piscina madrid", "tienda", "tienda de", "ventas", "venta de", "s.l.", " s.l", "sl", "sa", "s.a.", "slne")
                    val genericNames = listOf("swimming pool", "piscina", "pool")
                    val isGenericName = genericNames.any { name.trim() == it }
                    !isGenericName && excludePatterns.none { pattern -> name.contains(pattern) }
                }
                .map { place ->
                    val isHidden = HiddenPoolsManager.isHidden(context, place.placeId)
                    val isFavorite = FavoritesManager.isFavorite(context, place.placeId)
                    place.toDomain().copy(isHidden = isHidden, isFavorite = isFavorite)
                }

            try {
                poolCacheDao.insertPools(pools.map { it.toCacheEntity() })
            } catch (_: Exception) { /* cache write failure is non-fatal */ }
            return Result.success(pools)
        } catch (e: Exception) {
            val ttlThreshold = System.currentTimeMillis() - BASIC_TTL_MS
            val latDelta = radius / 111_000.0
            val lngDelta = radius / (111_000.0 * cos(Math.toRadians(lat)))
            val cached = poolCacheDao.getPoolsInBoundingBox(
                minLat = lat - latDelta, maxLat = lat + latDelta,
                minLng = lng - lngDelta, maxLng = lng + lngDelta,
                minCachedAt = ttlThreshold
            )
            if (cached.isNotEmpty()) {
                Result.success(cached.map { entity ->
                    val isHidden = HiddenPoolsManager.isHidden(context, entity.placeId)
                    val isFavorite = FavoritesManager.isFavorite(context, entity.placeId)
                    entity.toDomain().copy(isHidden = isHidden, isFavorite = isFavorite)
                })
            } else {
                Result.failure(e)
            }
        }
    }

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

    companion object {
        private const val BASIC_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
