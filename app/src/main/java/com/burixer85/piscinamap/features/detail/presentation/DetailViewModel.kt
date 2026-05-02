package com.burixer85.piscinamap.features.detail.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.detail.domain.repository.DetailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val detailRepository: DetailRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val poolId: String = savedStateHandle.get<String>("poolId") ?: ""

    private val _uiState = MutableStateFlow(DetailUiState(isLoading = true))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadPoolDetails()
    }

    private fun loadPoolDetails() {
        viewModelScope.launch {
            val result = detailRepository.getPoolDetails(poolId)
            result.fold(
                onSuccess = { pool ->
                    _uiState.update { it.copy(pool = pool, isLoading = false, error = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadPoolDetails()
    }
}

data class DetailUiState(
    val isLoading: Boolean = false,
    val pool: Pool? = null,
    val error: String? = null
)