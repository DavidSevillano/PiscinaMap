package com.burixer85.piscinamap.features.explore.domain.usecases

import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.explore.domain.repository.ExploreRepository
import javax.inject.Inject

class GetExploreNearbyPoolsUseCase @Inject constructor(
    private val repository: ExploreRepository
) {
    suspend operator fun invoke(lat: Double, lng: Double, radius: Int = 50000): Result<List<Pool>> {
        return repository.searchNearbyPools(lat, lng, radius)
    }
}