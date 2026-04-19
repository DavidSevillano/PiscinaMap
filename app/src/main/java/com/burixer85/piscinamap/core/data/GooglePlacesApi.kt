package com.burixer85.piscinamap.core.data

import com.burixer85.piscinamap.core.data.dto.PlacesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePlacesApi {
    @GET("maps/api/place/nearbysearch/json")
    suspend fun getNearbyPools(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("keyword") keyword: String = "swimming_pool",
        @Query("key") apiKey: String
    ): PlacesResponse
}