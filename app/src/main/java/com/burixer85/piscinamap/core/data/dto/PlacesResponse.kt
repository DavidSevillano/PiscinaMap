package com.burixer85.piscinamap.core.data.dto

import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.domain.model.Review
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
        photoUrls = this.photos?.map { it.photoReference } ?: emptyList(),
        isNew = false
    )
}

@Serializable
data class PlaceDetailsResponse(
    @SerialName("result") val result: PlaceDetailsDto? = null,
    @SerialName("status") val status: String
)

@Serializable
data class PlaceDetailsDto(
    @SerialName("name") val name: String,
    @SerialName("geometry") val geometry: GeometryDto,
    @SerialName("formatted_address") val address: String? = null,
    @SerialName("rating") val rating: Float? = null,
    @SerialName("opening_hours") val openingHours: OpeningHoursDetailDto? = null,
    @SerialName("formatted_phone_number") val formattedPhoneNumber: String? = null,
    @SerialName("reviews") val reviews: List<ReviewDto>? = null,
    @SerialName("types") val types: List<String> = emptyList(),
    @SerialName("photos") val photos: List<PhotoDto>? = null
)

@Serializable
data class OpeningHoursDetailDto(
    @SerialName("weekday_text") val weekdayText: List<String>? = null,
    @SerialName("open_now") val openNow: Boolean? = null
)

@Serializable
data class ReviewDto(
    @SerialName("author_name") val authorName: String,
    @SerialName("rating") val rating: Float,
    @SerialName("text") val text: String,
    @SerialName("relative_time_description") val relativeTimeDescription: String,
    @SerialName("language") val language: String? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null
)

fun PlaceDetailsDto.toDomain(): Pool {
    val services = types.mapNotNull { type ->
        when (type) {
            "swimming_pool" -> "Piscina"
            "gym" -> "Gimnasio"
            "spa" -> "Spa"
            "parking" -> "Parking"
            "restaurant" -> "Restaurante"
            "cafe" -> "Cafetería"
            "changing_room" -> "Vestuarios"
            "shower" -> "Duchas"
            "locker" -> "Taqui"
            "lifeguard" -> "Socorrista"
            "water_slide" -> "Tobogán"
            "kids_pool" -> "Piscina infantil"
            else -> null
        }
    }

    val weekdayText = this.openingHours?.weekdayText ?: emptyList()
    val currentOpeningHours = weekdayText.firstOrNull()

    return Pool(
        id = "",
        name = this.name,
        latitude = this.geometry.location.lat,
        longitude = this.geometry.location.lng,
        address = this.address ?: "",
        rating = this.rating,
        isOpenNow = this.openingHours?.openNow,
        photoUrls = this.photos?.map { it.photoReference } ?: emptyList(),
        formattedPhone = this.formattedPhoneNumber,
        openingHours = weekdayText,
        currentOpeningHours = currentOpeningHours,
        services = services,
        reviews = this.reviews?.map {
            Review(
                it.authorName,
                it.rating,
                it.text,
                it.relativeTimeDescription,
                it.language,
                it.profilePhotoUrl
            )
        } ?: emptyList()
    )
}