package com.burixer85.piscinamap.di

import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.dto.PlaceDetailsResponse
import com.burixer85.piscinamap.core.data.dto.PlacesResponse
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.di.NetworkModule
import com.burixer85.piscinamap.features.detail.domain.repository.DetailRepository
import com.burixer85.piscinamap.features.explore.domain.repository.ExploreRepository
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import com.burixer85.piscinamap.util.TestData
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.awaitCancellation
import javax.inject.Singleton

class FakePoolRepository : PoolRepository {
    var shouldBlock = false
    var listResult: Result<List<Pool>> = Result.success(emptyList())
    var detailResult: Result<Pool> = Result.success(TestData.pool)

    override suspend fun searchNearbyPools(lat: Double, lng: Double, radius: Int): Result<List<Pool>> {
        if (shouldBlock) awaitCancellation()
        return listResult
    }

    override suspend fun getPoolDetails(placeId: String): Result<Pool> {
        if (shouldBlock) awaitCancellation()
        return detailResult
    }
}

class FakeExploreRepository : ExploreRepository {
    var shouldBlock = false
    var result: Result<List<Pool>> = Result.success(emptyList())
    override suspend fun searchNearbyPools(lat: Double, lng: Double, radius: Int): Result<List<Pool>> {
        if (shouldBlock) awaitCancellation()
        return result
    }
}

class FakeDetailRepository : DetailRepository {
    var shouldBlock = false
    var result: Result<Pool> = Result.success(TestData.pool)
    override suspend fun getPoolDetails(placeId: String): Result<Pool> {
        if (shouldBlock) awaitCancellation()
        return result
    }
}

@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
@Module
object FakeRepositoryModule {
    val fakePoolRepository = FakePoolRepository()
    val fakeExploreRepository = FakeExploreRepository()
    val fakeDetailRepository = FakeDetailRepository()

    @Provides @Singleton
    fun provideGooglePlacesApi(): GooglePlacesApi {
        return object : GooglePlacesApi {
            override suspend fun getNearbyPools(
                location: String,
                radius: Int,
                keyword: String,
                language: String,
                apiKey: String
            ): PlacesResponse {
                throw UnsupportedOperationException("Not implemented in test")
            }

            override suspend fun getPlaceDetails(
                placeId: String,
                fields: String,
                language: String,
                apiKey: String
            ): PlaceDetailsResponse {
                throw UnsupportedOperationException("Not implemented in test")
            }
        }
    }

    @Provides @Singleton
    fun providePoolRepository(): PoolRepository = fakePoolRepository

    @Provides @Singleton
    fun provideExploreRepository(): ExploreRepository = fakeExploreRepository

    @Provides @Singleton
    fun provideDetailRepository(): DetailRepository = fakeDetailRepository
}
