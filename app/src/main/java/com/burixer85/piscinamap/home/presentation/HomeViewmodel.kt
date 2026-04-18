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
            try {
                Log.d("DEBUG_PISCINA", "1. Iniciando carga en Sevilla...")
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                delay(1500)

                val mockPools = listOf(
                    Pool(
                        id = "sev1",
                        name = "Centro Deportivo San Pablo",
                        latitude = 37.3970,
                        longitude = -5.9688,
                        address = "Av. de Kansas City, s/n, 41007 Sevilla",
                        rating = 4.2f,
                        isOpenNow = true,
                        photoUrl = null
                    ),
                    Pool(
                        id = "sev2",
                        name = "Piscina Municipal Tiro de Línea",
                        latitude = 37.3698,
                        longitude = -5.9772,
                        address = "Calle Lora del Río, s/n, 41013 Sevilla",
                        rating = 4.0f,
                        isOpenNow = false,
                        photoUrl = null
                    )
                )

                Log.d("DEBUG_PISCINA", "2. Piscinas de Sevilla creadas")

                _uiState.update {
                    it.copy(pools = mockPools)
                }

            } catch (e: Exception) {
                Log.e("DEBUG_PISCINA", "Error: ${e.message}")
            } finally {
                Log.d("DEBUG_PISCINA", "3. Cargador apagado")
                _uiState.update { it.copy(isLoading = false) }
            }
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