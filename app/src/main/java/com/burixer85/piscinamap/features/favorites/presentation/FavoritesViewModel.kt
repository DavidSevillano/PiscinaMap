package com.burixer85.piscinamap.features.favorites.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
import com.burixer85.piscinamap.core.presentation.util.PoolStateManager
import com.burixer85.piscinamap.features.detail.domain.repository.DetailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val pools: List<Pool> = emptyList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detailRepository: DetailRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState(isLoading = true))
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val fetchingIds = mutableSetOf<String>()

    private val favoriteStateListener: (String, Boolean) -> Unit = { poolId, isFavorite ->
        updateFavoriteState(poolId, isFavorite)
    }

    init {
        PoolStateManager.subscribeFavorite(favoriteStateListener)
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            val ids = FavoritesManager.getFavoriteIds(context)
            if (ids.isEmpty()) {
                _uiState.update { FavoritesUiState(isEmpty = true, isLoading = false) }
                return@launch
            }
            val pools = ids.mapNotNull { id ->
                detailRepository.getPoolDetails(id).getOrNull()?.copy(isFavorite = true)
            }
            _uiState.update { current ->
                current.copy(
                    pools = pools,
                    isEmpty = pools.isEmpty(),
                    isLoading = false,
                )
            }
        }
    }

    internal fun updateFavoriteState(poolId: String, isFavorite: Boolean) {
        if (!isFavorite) {
            // Pool was un-favorited elsewhere — remove it from the list
            _uiState.update { current ->
                val updated = current.pools.filter { it.id != poolId }
                current.copy(pools = updated, isEmpty = updated.isEmpty())
            }
        } else {
            // Pool was favorited elsewhere — load and add it if not already present
            if (_uiState.value.pools.none { it.id == poolId } && poolId !in fetchingIds) {
                fetchingIds.add(poolId)
                viewModelScope.launch {
                    try {
                        detailRepository.getPoolDetails(poolId).getOrNull()
                            ?.copy(isFavorite = true)
                            ?.let { pool ->
                                _uiState.update { current ->
                                    current.copy(
                                        pools = current.pools + pool,
                                        isEmpty = false,
                                    )
                                }
                            }
                    } finally {
                        fetchingIds.remove(poolId)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        PoolStateManager.unsubscribeFavorite(favoriteStateListener)
    }
}
