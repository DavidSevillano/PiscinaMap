package com.burixer85.piscinamap.core.data.dto

import com.burixer85.piscinamap.core.domain.model.Pool
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlacesResponse(
    @SerialName("results") val results: List<PlaceDto>,
    @SerialName("status") val status: String,
    @SerialName("next_page_token") val nextPageToken: String? = null
)

@Serializable
data class PlaceDto(
    @SerialName("place_id") val placeId: String,
    @SerialName("name") val name: String,
    @SerialName("vicinity") val address: String? = null,
    @SerialName("geometry") val geometry: GeometryDto,
    @SerialName("rating") val rating: Float? = null,
    @SerialName("opening_hours") val openingHours: OpeningHoursDto? = null,
    @SerialName("photos") val photos: List<PhotoDto>? = null,
    @SerialName("types") val types: List<String> = emptyList()
)

@Serializable
data class GeometryDto(
    @SerialName("location") val location: LocationDto
)

@Serializable
data class LocationDto(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double
)

@Serializable
data class OpeningHoursDto(
    @SerialName("open_now") val openNow: Boolean? = null
)

@Serializable
data class PhotoDto(
    @SerialName("photo_reference") val photoReference: String
)

fun PlaceDto.toDomain(): Pool {
    return Pool(
        id = this.placeId,
        name = this.name,
        latitude = this.geometry.location.lat,
        longitude = this.geometry.location.lng,
        address = this.address ?: "",
        rating = this.rating,
        isOpenNow = this.openingHours?.openNow,
        photoUrl = this.photos?.firstOrNull()?.photoReference,
        isNew = false
    )
}