package com.burixer85.piscinamap.core.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.burixer85.piscinamap.core.data.local.entity.PoolCacheEntity

@Dao
interface PoolCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPools(pools: List<PoolCacheEntity>)

    @Query("""
        SELECT * FROM pool_cache
        WHERE latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLng AND :maxLng
          AND cached_at >= :minCachedAt
    """)
    suspend fun getPoolsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        minCachedAt: Long
    ): List<PoolCacheEntity>
}
