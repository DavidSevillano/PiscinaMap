package com.burixer85.piscinamap.core.domain.model

data class Pool(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val rating: Float?,
    val isOpenNow: Boolean?,
    val photoUrls: List<String> = emptyList(),
    val isNew: Boolean = false,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val formattedPhone: String? = null,
    val openingHours: List<String> = emptyList(),
    val currentOpeningHours: String? = null,
    val services: List<String> = emptyList(),
    val reviews: List<Review> = emptyList()
) {
    val photoUrl: String?
        get() = photoUrls.firstOrNull()
}

data class Review(
    val authorName: String,
    val rating: Float,
    val text: String,
    val relativeTimeDescription: String,
    val publishedAt: Long = 0L,
    val language: String? = null,
    val profilePhotoUrl: String? = null
)