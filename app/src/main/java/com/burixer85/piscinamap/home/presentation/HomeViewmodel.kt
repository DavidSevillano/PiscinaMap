package com.burixer85.piscinamap.home.presentation

import android.content.Context
import android.location.Geocoder
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var sessionToken: AutocompleteSessionToken? = null

    fun onMapMoved(currentCenter: LatLng, isCameraMoving: Boolean) {
        val lastLocation = lastSearchLocation

        if (isCameraMoving || lastLocation == null) {
            _showSearchButton.value = false
            return
        }

        val results = FloatArray(1)
        Location.distanceBetween(
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

    fun onPredictionSelected(prediction: AutocompletePrediction, context: Context) {
        val placesClient = Places.createClient(context)

        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

        val request = FetchPlaceRequest.builder(prediction.placeId, placeFields)
            .setSessionToken(sessionToken)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                place.latLng?.let { latLng ->
                    sessionToken = null

                    _uiState.update { it.copy(
                        searchText = place.name ?: "",
                        predictions = emptyList(),
                        searchLocationResult = latLng
                    )}

                    fetchPools(latLng.latitude, latLng.longitude, isManual = true)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PLACES", "Error al obtener detalles del lugar: ${e.message}")
                _uiState.update { it.copy(errorMessage = "No se pudieron obtener los detalles del lugar") }
            }
    }

    fun onSearchTextChange(newText: String, context: Context) {
        _uiState.update { it.copy(searchText = newText) }

        if (newText.isBlank() || newText.length < 3) {
            _uiState.update { it.copy(predictions = emptyList()) }
            return
        }

        if (sessionToken == null) sessionToken = AutocompleteSessionToken.newInstance()

        val placesClient = Places.createClient(context)
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(newText)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                _uiState.update { it.copy(predictions = response.autocompletePredictions) }
            }
            .addOnFailureListener { e -> Log.e("PLACES", "Error: ${e.message}") }
    }

    fun onSearchLocationProcessed() {
        _uiState.update { it.copy(searchLocationResult = null) }
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

    fun clearPredictions() {
        _uiState.update { it.copy(predictions = emptyList()) }
    }
}

data class MapUiState(
    val isLoading: Boolean = false,
    val pools: List<Pool> = emptyList(),
    val userLocation: LatLng? = null,
    val searchText: String = "",
    val searchLocationResult: LatLng? = null,
    val predictions: List<AutocompletePrediction> = emptyList(),
    val errorMessage: String? = null
)