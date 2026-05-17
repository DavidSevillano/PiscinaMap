package com.burixer85.piscinamap.features.explore.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.domain.model.FilterState
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.explore.domain.usecases.GetExploreNearbyPoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getExploreNearbyPoolsUseCase: GetExploreNearbyPoolsUseCase,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    val filteredPools: StateFlow<List<Pool>> = _uiState
        .map { state -> applyFilters(state.pools, state.filters, state.userLatLng) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0

    fun setUserLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(userLatLng = lat to lng) }
    }

    fun updateFilter(filters: FilterState) {
        _uiState.update { it.copy(filters = filters) }
    }

    fun fetchPools(lat: Double, lng: Double) {
        currentLat = lat
        currentLng = lng

        analytics.trackScreen("ExploreScreen")
        analytics.trackEvent(
            "map_area_searched",
            mapOf("latitude" to lat.toString(), "longitude" to lng.toString())
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getExploreNearbyPoolsUseCase(lat, lng, 50000).fold(
                onSuccess = { pools ->
                    _uiState.update { it.copy(pools = pools, isLoading = false, error = null) }
                },
                onFailure = { error ->
                    analytics.logNonFatalError(error)
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    internal fun applyFilters(
        pools: List<Pool>,
        filters: FilterState,
        userLatLng: Pair<Double, Double>?
    ): List<Pool> = pools.filter { pool ->
        val ratingOk = filters.minRating?.let { (pool.rating ?: 0f) >= it } ?: true
        val openOk = !filters.openNow || pool.isOpenNow == true
        val distanceOk = filters.maxDistanceKm?.let { maxKm ->
            userLatLng?.let { (uLat, uLng) ->
                haversineKm(uLat, uLng, pool.latitude, pool.longitude) <= maxKm
            } ?: true
        } ?: true
        val typeOk = filters.selectedTypes.isEmpty() || pool.poolType in filters.selectedTypes
        ratingOk && openOk && distanceOk && typeOk
    }

    internal fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }
}

data class ExploreUiState(
    val isLoading: Boolean = false,
    val pools: List<Pool> = emptyList(),
    val error: String? = null,
    val filters: FilterState = FilterState(),
    val userLatLng: Pair<Double, Double>? = null
)
