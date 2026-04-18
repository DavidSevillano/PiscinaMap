package com.burixer85.piscinamap.core.di

import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.home.data.repository.PoolRepositoryImpl
import com.burixer85.piscinamap.home.domain.repository.PoolRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGoogleApi(): GooglePlacesApi {
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApi::class.java)
    }

    @Provides
    @Singleton
    fun providePoolRepository(api: GooglePlacesApi): PoolRepository {
        return PoolRepositoryImpl(api)
    }
}