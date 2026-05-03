package com.burixer85.piscinamap.core.di

import android.content.Context
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.features.home.data.repository.PoolRepositoryImpl
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import com.burixer85.piscinamap.features.detail.data.repository.DetailRepositoryImpl
import com.burixer85.piscinamap.features.detail.domain.repository.DetailRepository
import com.burixer85.piscinamap.features.explore.data.repository.ExploreRepositoryImpl
import com.burixer85.piscinamap.features.explore.domain.repository.ExploreRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApi::class.java)
    }

    @Provides
    @Singleton
    fun providePoolRepository(api: GooglePlacesApi, @ApplicationContext context: Context): PoolRepository {
        return PoolRepositoryImpl(api, context)
    }

    @Provides
    @Singleton
    fun provideDetailRepository(api: GooglePlacesApi): DetailRepository {
        return DetailRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideExploreRepository(api: GooglePlacesApi, @ApplicationContext context: Context): ExploreRepository {
        return ExploreRepositoryImpl(api, context)
    }
}