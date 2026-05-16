package com.burixer85.piscinamap.core.di

import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.analytics.NoOpAnalyticsManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsManager(impl: NoOpAnalyticsManager): AnalyticsManager
}
