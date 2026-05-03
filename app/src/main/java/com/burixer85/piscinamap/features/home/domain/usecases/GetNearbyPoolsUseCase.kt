package com.burixer85.piscinamap.features.home.domain.usecases

import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import javax.inject.Inject

class GetNearbyPoolsUseCase @Inject constructor(
    private val repository: PoolRepository
) {
    suspend operator fun invoke(lat: Double, lng: Double, radius: Int = 2500): Result<List<Pool>> {
        return repository.searchNearbyPools(lat, lng, radius)
    }
}