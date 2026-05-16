package com.burixer85.piscinamap.core.di

import android.content.Context
import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.analytics.FirebaseAnalyticsManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsManager(impl: FirebaseAnalyticsManager): AnalyticsManager

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
            FirebaseAnalytics.getInstance(context)

        @Provides
        @Singleton
        fun provideFirebaseCrashlytics(): FirebaseCrashlytics =
            FirebaseCrashlytics.getInstance()
    }
}
