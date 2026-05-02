package com.burixer85.piscinamap.features.detail.domain.usecases

import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import javax.inject.Inject

class GetPoolDetailsUseCase @Inject constructor(
    private val repository: PoolRepository
) {
    suspend operator fun invoke(placeId: String): Result<Pool> {
        return repository.getPoolDetails(placeId)
    }
}