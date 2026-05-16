package com.burixer85.piscinamap.core.di

import android.content.Context
import androidx.room.Room
import com.burixer85.piscinamap.core.data.local.db.PoolCacheDao
import com.burixer85.piscinamap.core.data.local.db.PoolDatabase
import com.burixer85.piscinamap.core.data.local.db.PoolDetailCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePoolDatabase(@ApplicationContext context: Context): PoolDatabase =
        Room.databaseBuilder(context, PoolDatabase::class.java, "pool_cache.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePoolCacheDao(db: PoolDatabase): PoolCacheDao = db.poolCacheDao()

    @Provides
    fun providePoolDetailCacheDao(db: PoolDatabase): PoolDetailCacheDao = db.poolDetailCacheDao()
}
