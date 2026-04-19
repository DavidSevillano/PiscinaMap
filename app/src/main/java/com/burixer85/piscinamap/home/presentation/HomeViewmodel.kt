package com.burixer85.piscinamap.home.presentation

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

    fun fetchPools(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = getNearbyPoolsUseCase(latitude, longitude)

            result.fold(
                onSuccess = { poolsList ->
                    Log.d("DEBUG_MAPA", "Enviando ${poolsList.size} piscinas a la UI")
                    _uiState.update { it.copy(pools = poolsList, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Error al cargar piscinas"
                        )
                    }
                }
            )
        }
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