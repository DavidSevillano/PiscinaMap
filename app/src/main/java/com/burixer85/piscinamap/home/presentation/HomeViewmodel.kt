package com.burixer85.piscinamap.home.presentation

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.PoolUtils.getGooglePhotoUrl
import com.burixer85.piscinamap.home.domain.usecases.GetNearbyPoolsUseCase
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNearbyPoolsUseCase: GetNearbyPoolsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState(isLoading = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _events = kotlinx.coroutines.channels.Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    private var lastSearchLocation: LatLng? = null

    private var sessionToken: AutocompleteSessionToken? = null

    private var lastMoveTime = 0L

    fun onMapMoved(currentCenter: LatLng, isCameraMoving: Boolean) {
        val currentTime = System.currentTimeMillis()
        if (isCameraMoving && (currentTime - lastMoveTime < 150)) return
        lastMoveTime = currentTime

        val lastLocation = lastSearchLocation

        if (isCameraMoving || lastLocation == null) {
            if (_uiState.value.showSearchButton) {
                _uiState.update { it.copy(showSearchButton = false) }
            }
            return
        }

        val results = FloatArray(1)
        Location.distanceBetween(
            lastLocation.latitude, lastLocation.longitude,
            currentCenter.latitude, currentCenter.longitude,
            results
        )

        val shouldShow = results[0] > 1500
        if (_uiState.value.showSearchButton != shouldShow) {
            _uiState.update { it.copy(showSearchButton = shouldShow) }
        }
    }


    private fun preloadPoolImages(context: Context, pools: List<Pool>) {
        val imageLoader = Coil.imageLoader(context)
        pools.forEach { pool ->
            val fullUrl = getGooglePhotoUrl(pool.photoUrl)

            fullUrl?.let { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    fun fetchPools(latitude: Double, longitude: Double, context: Context, isManual: Boolean = false) {
        val newLocation = LatLng(latitude, longitude)

        _uiState.update { it.copy(isLoading = true, errorMessage = null, showSearchButton = false) }

        viewModelScope.launch {
            val result = getNearbyPoolsUseCase(latitude, longitude)

            result.fold(
                onSuccess = { incomingPools ->
                    preloadPoolImages(context, incomingPools)

                    val currentPoolIds = _uiState.value.pools.map { it.id }.toSet()

                    val realNewPools = incomingPools.filter { it.id !in currentPoolIds }

                    if (isManual) {
                        val message = if (realNewPools.isNotEmpty()) {
                            "¡Se han encontrado ${realNewPools.size} nuevas piscinas!"
                        } else {
                            "No hay piscinas nuevas en esta zona"
                        }
                        _events.send(HomeEvent.ShowToast(message))
                    }

                    lastSearchLocation = newLocation

                    _uiState.update { currentState ->
                        val finalPools = if (isManual) {
                            val oldPools = currentState.pools.map { it.copy(isNew = false) }
                            val newPools = incomingPools.map { pool ->
                                pool.copy(isNew = pool.id !in currentPoolIds)
                            }
                            (oldPools + newPools).distinctBy { it.id }
                        } else {
                            incomingPools.map { it.copy(isNew = false) }
                        }
                        currentState.copy(pools = finalPools, isLoading = false)
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    fun onPredictionSelected(prediction: AutocompletePrediction, context: Context) {
        _uiState.update { it.copy(isLoading = true, predictions = emptyList()) }

        val placesClient = Places.createClient(context)
        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.builder(prediction.placeId, placeFields)
            .setSessionToken(sessionToken)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val latLng = response.place.latLng
                if (latLng != null) {
                    sessionToken = null
                    _uiState.update { it.copy(
                        searchText = response.place.name ?: "",
                    ) }

                    viewModelScope.launch {
                        _events.send(HomeEvent.AnimateToLocation(latLng))
                    }
                    fetchPools(latLng.latitude, latLng.longitude, context = context, isManual = true)
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Ubicación no disponible") }
                }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Error al conectar con el servidor"
                ) }
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

    fun onMarkerClicked(poolId: String) {
        val selectedPool = _uiState.value.pools.find { it.id == poolId }

        selectedPool?.let { pool ->
            viewModelScope.launch {
                _events.send(HomeEvent.AnimateToLocation(LatLng(pool.latitude, pool.longitude)))
            }
        }

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
    val showSearchButton: Boolean = false,
    val errorMessage: String? = null
)

sealed class HomeEvent {
    data class AnimateToLocation(val latLng: LatLng) : HomeEvent()
    data class ShowToast(val message: String, val isLong: Boolean = false) : HomeEvent()
}