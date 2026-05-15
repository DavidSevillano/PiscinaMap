package com.burixer85.piscinamap.core.data

import com.burixer85.piscinamap.core.data.dto.PlaceDetailsResponse
import com.burixer85.piscinamap.core.data.dto.PlacesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePlacesApi {
    @GET("maps/api/place/nearbysearch/json")
    suspend fun getNearbyPools(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("keyword") keyword: String = "swimming_pool",
        @Query("language") language: String,
        @Query("key") apiKey: String
    ): PlacesResponse

    @GET("maps/api/place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "name,geometry,formatted_address,rating,opening_hours,formatted_phone_number,reviews,types,photos",
        @Query("language") language: String,
        @Query("key") apiKey: String
    ): PlaceDetailsResponse
}