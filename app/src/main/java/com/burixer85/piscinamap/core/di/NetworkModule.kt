package com.burixer85.piscinamap.core.di

import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.PoolSearchDataSource
import com.burixer85.piscinamap.core.data.local.db.PoolDetailCacheDao
import com.burixer85.piscinamap.features.detail.data.repository.DetailRepositoryImpl
import com.burixer85.piscinamap.features.detail.domain.repository.DetailRepository
import com.burixer85.piscinamap.features.explore.data.repository.ExploreRepositoryImpl
import com.burixer85.piscinamap.features.explore.domain.repository.ExploreRepository
import com.burixer85.piscinamap.features.home.data.repository.PoolRepositoryImpl
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
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
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-Android-Package", BuildConfig.APPLICATION_ID)
                    .addHeader("X-Android-Cert", BuildConfig.SIGNING_CERT_SHA1)
                    .build()
                chain.proceed(request)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApi::class.java)
    }

    @Provides
    @Singleton
    fun providePoolRepository(
        api: GooglePlacesApi,
        poolSearchDataSource: PoolSearchDataSource
    ): PoolRepository = PoolRepositoryImpl(api, poolSearchDataSource)

    @Provides
    @Singleton
    fun provideDetailRepository(
        api: GooglePlacesApi,
        poolDetailCacheDao: PoolDetailCacheDao
    ): DetailRepository = DetailRepositoryImpl(api, poolDetailCacheDao)

    @Provides
    @Singleton
    fun provideExploreRepository(
        poolSearchDataSource: PoolSearchDataSource
    ): ExploreRepository = ExploreRepositoryImpl(poolSearchDataSource)
}