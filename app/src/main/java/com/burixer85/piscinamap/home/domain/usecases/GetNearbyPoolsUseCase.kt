package com.burixer85.piscinamap.home.domain.usecases

import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.home.domain.repository.PoolRepository
import javax.inject.Inject

class GetNearbyPoolsUseCase @Inject constructor(
    private val repository: PoolRepository
) {
    suspend operator fun invoke(lat: Double, lng: Double): Result<List<Pool>> {
        return repository.searchNearbyPools(lat, lng, 2500)
    }
}