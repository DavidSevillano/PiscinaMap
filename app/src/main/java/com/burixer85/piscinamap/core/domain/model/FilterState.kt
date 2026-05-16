package com.burixer85.piscinamap.core.domain.model

data class FilterState(
    val minRating: Float? = null,
    val openNow: Boolean = false,
    val maxDistanceKm: Int? = null,
    val selectedTypes: Set<PoolType> = emptySet()
)
