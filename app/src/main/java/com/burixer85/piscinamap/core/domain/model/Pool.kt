package com.burixer85.piscinamap.core.domain.model

data class Pool(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val rating: Float?,
    val isOpenNow: Boolean?,
    val photoUrl: String?,
    val isNew: Boolean = false
)