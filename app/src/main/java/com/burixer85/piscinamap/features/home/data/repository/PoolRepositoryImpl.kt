package com.burixer85.piscinamap.features.home.data.repository

import android.content.Context
import android.util.Log
import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.dto.toDomain
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.HiddenPoolsManager
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import java.util.Locale
import javax.inject.Inject

class PoolRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi,
    private val context: Context
) : PoolRepository {
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

            when (response.status) {
                "REQUEST_DENIED" -> {
                    Log.e("POOLS", "API request denied - check API key")
                    return Result.failure(Exception("Error de API: Solicitud denegada"))
                }
                "OVER_QUERY_LIMIT" -> {
                    Log.e("POOLS", "API quota exceeded")
                    return Result.failure(Exception("Cuota de API excedida"))
                }
                "INVALID_REQUEST" -> {
                    Log.e("POOLS", "Invalid request")
                    return Result.failure(Exception("Solicitud inválida"))
                }
                "UNKNOWN_ERROR" -> {
                    Log.e("POOLS", "Server error")
                    return Result.failure(Exception("Error del servidor"))
                }
                "ZERO_RESULTS", "OK" -> {
                    // Continue processing
                }
                else -> {
                    Log.w("POOLS", "Unknown status: ${response.status}")
                }
            }

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

            Log.d("POOLS", "Filter result: ${pools.size} pools")
            response.results.take(3).forEach { place ->
                Log.d("POOLS", "Types: ${place.types} - ${place.name}")
            }

            Result.success(pools)
        } catch (e: Exception) {
            Log.e("DEBUG_MAPA", "Error al conectar con Google: ${e.message}")
            Result.failure(e)
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
            Log.e("POOL_DETAILS", "Error al obtener detalles: ${e.message}")
            Result.failure(e)
        }
    }
}