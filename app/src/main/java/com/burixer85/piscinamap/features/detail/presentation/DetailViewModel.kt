package com.burixer85.piscinamap.features.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.detail.domain.usecases.GetPoolDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getPoolDetailsUseCase: GetPoolDetailsUseCase
) : ViewModel() {

    private var poolId: String = ""

    private val _uiState = MutableStateFlow(DetailUiState(isLoading = true))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun setPoolId(id: String) {
        if (id != poolId) {
            poolId = id
            loadPoolDetails()
        }
    }

    private fun loadPoolDetails() {
        if (poolId.isBlank()) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = getPoolDetailsUseCase(poolId)
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
        loadPoolDetails()
    }
}

data class DetailUiState(
    val isLoading: Boolean = false,
    val pool: Pool? = null,
    val error: String? = null
)