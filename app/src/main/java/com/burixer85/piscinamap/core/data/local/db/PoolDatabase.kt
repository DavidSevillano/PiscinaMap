package com.burixer85.piscinamap.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.burixer85.piscinamap.core.data.local.converter.RoomConverters
import com.burixer85.piscinamap.core.data.local.entity.PoolCacheEntity
import com.burixer85.piscinamap.core.data.local.entity.PoolDetailCacheEntity

@Database(
    entities = [PoolCacheEntity::class, PoolDetailCacheEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class PoolDatabase : RoomDatabase() {
    abstract fun poolCacheDao(): PoolCacheDao
    abstract fun poolDetailCacheDao(): PoolDetailCacheDao
}
