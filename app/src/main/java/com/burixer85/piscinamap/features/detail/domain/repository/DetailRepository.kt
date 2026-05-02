package com.burixer85.piscinamap.features.detail.domain.repository

import com.burixer85.piscinamap.core.domain.model.Pool

interface DetailRepository {
    suspend fun getPoolDetails(placeId: String): Result<Pool>
}