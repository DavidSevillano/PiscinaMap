package com.burixer85.piscinamap.home.presentation

import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.home.domain.usecases.GetNearbyPoolsUseCase
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNearbyPoolsUseCase: GetNearbyPoolsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState(isLoading = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _showSearchButton = MutableStateFlow(false)
    val showSearchButton = _showSearchButton.asStateFlow()

    private val _searchTriggeredManually = MutableStateFlow(false)
    val searchTriggeredManually = _searchTriggeredManually.asStateFlow()

    private var lastSearchLocation: LatLng? = null

    private fun calculateDistance(loc1: LatLng, loc2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            loc1.latitude, loc1.longitude,
            loc2.latitude, loc2.longitude,
            results
        )
        return results[0]
    }

    fun onMapMoved(currentCenter: LatLng, isCameraMoving: Boolean) {
        val lastLocation = lastSearchLocation

        if (isCameraMoving || lastLocation == null) {
            _showSearchButton.value = false
            return
        }

        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            lastLocation.latitude, lastLocation.longitude,
            currentCenter.latitude, currentCenter.longitude,
            results
        )

        _showSearchButton.value = results[0] > 1500
    }

    fun fetchPools(latitude: Double, longitude: Double, isManual: Boolean = false) {
        val newLocation = LatLng(latitude, longitude)

        if (isManual) {
            _searchTriggeredManually.value = true
        }

        _showSearchButton.value = false

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = getNearbyPoolsUseCase(latitude, longitude)

            result.fold(
                onSuccess = { incomingPools ->
                    lastSearchLocation = newLocation

                    _uiState.update { currentState ->
                        val finalPools = if (isManual) {
                            val oldPools = currentState.pools.map { it.copy(isNew = false) }
                            val newPools = incomingPools.map { it.copy(isNew = true) }
                            (oldPools + newPools).distinctBy { it.id }
                        } else {
                            incomingPools.map { it.copy(isNew = false) }
                        }

                        currentState.copy(
                            pools = finalPools,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Error desconocido")
                    }
                }
            )
        }
    }

    fun onMarkerClicked(poolId: String) {
        _uiState.update { currentState ->
            val updatedPools = currentState.pools.map { pool ->
                if (pool.id == poolId) {
                    pool.copy(isNew = false)
                } else {
                    pool
                }
            }
            currentState.copy(pools = updatedPools)
        }
    }

    fun clearManualSearchFlag() {
        _searchTriggeredManually.value = false
    }

    fun updateLocation(location: LatLng) {
        _uiState.update { it.copy(userLocation = location) }
    }

    fun onSearchTextChange(newText: String) {
        _uiState.update { it.copy(searchText = newText) }
    }

    fun onRetry(lat: Double, lng: Double) {
        fetchPools(lat, lng)
    }
}

data class MapUiState(
    val isLoading: Boolean = false,
    val pools: List<Pool> = emptyList(),
    val userLocation: LatLng? = null,
    val searchText: String = "",
    val errorMessage: String? = null
)