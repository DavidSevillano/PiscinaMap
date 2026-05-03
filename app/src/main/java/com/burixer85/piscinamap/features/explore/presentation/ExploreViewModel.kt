package com.burixer85.piscinamap.features.explore.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.explore.domain.usecases.GetExploreNearbyPoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getExploreNearbyPoolsUseCase: GetExploreNearbyPoolsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0

    fun fetchPools(lat: Double, lng: Double) {
        currentLat = lat
        currentLng = lng

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    warning = null,
                    hasSearchedMore = false
                )
            }
            getExploreNearbyPoolsUseCase(lat, lng, 50000).fold(
                onSuccess = { pools ->
                    _uiState.update { it.copy(pools = pools, isLoading = false, error = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    fun fetchMorePools() {
        if (_uiState.value.isLoadingMore || _uiState.value.hasSearchedMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null, hasSearchedMore = true) }
            getExploreNearbyPoolsUseCase(currentLat, currentLng, 50000).fold(
                onSuccess = { newPools ->
                    val existingIds = _uiState.value.pools.map { it.id }.toSet()
                    val uniqueNewPools = newPools.filter { it.id !in existingIds }

                    _uiState.update {
                        it.copy(
                            pools = it.pools + uniqueNewPools,
                            isLoadingMore = false,
                            warning = if (uniqueNewPools.isEmpty()) {
                                "No se encontraron más piscinas en esta área."
                            } else null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoadingMore = false, error = error.message) }
                }
            )
        }
    }
}

data class ExploreUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val pools: List<Pool> = emptyList(),
    val error: String? = null,
    val warning: String? = null,
    val hasSearchedMore: Boolean = false
)