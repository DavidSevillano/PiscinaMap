package com.burixer85.piscinamap.features.explore.data.repository

import android.content.Context
import android.util.Log
import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.dto.toDomain
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.HiddenPoolsManager
import com.burixer85.piscinamap.features.explore.domain.repository.ExploreRepository
import javax.inject.Inject

class ExploreRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi,
    private val context: Context
) : ExploreRepository {

    override suspend fun searchNearbyPools(
        lat: Double,
        lng: Double,
        radius: Int
    ): Result<List<Pool>> {
        return try {
            val response = api.getNearbyPools(
                location = "$lat,$lng",
                radius = radius,
                keyword = "swimming_pool",
                apiKey = BuildConfig.GOOGLEMAPS_KEY
            )

            val pools = response.results
                .filter { place ->
                    val name = place.name.lowercase()
                    val excludePatterns = listOf(
                        "piscinas",
                        "piscina s",
                        "piscinas triana",
                        "acuaeuropa",
                        "piscinas sevilla",
                        "piscina sevilla",
                        "piscina madrid",
                        "tienda",
                        "tienda de",
                        "ventas",
                        "venta de",
                        "s.l.",
                        " s.l",
                        "sl",
                        "sa",
                        "s.a.",
                        "slne"
                    )
                    excludePatterns.none { pattern -> name.contains(pattern) }
                }
                .map { place ->
                    val isHidden = HiddenPoolsManager.isHidden(context, place.placeId)
                    place.toDomain().copy(isHidden = isHidden)
                }

            Result.success(pools)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}