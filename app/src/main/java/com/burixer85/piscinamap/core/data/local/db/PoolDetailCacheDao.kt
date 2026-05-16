package com.burixer85.piscinamap.core.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.burixer85.piscinamap.core.data.local.entity.PoolDetailCacheEntity

@Dao
interface PoolDetailCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: PoolDetailCacheEntity)

    @Query("SELECT * FROM pool_detail_cache WHERE place_id = :placeId AND cached_at >= :minCachedAt LIMIT 1")
    suspend fun getDetail(placeId: String, minCachedAt: Long): PoolDetailCacheEntity?

    @Query("SELECT * FROM pool_detail_cache WHERE place_id = :placeId LIMIT 1")
    suspend fun getDetailIgnoringTtl(placeId: String): PoolDetailCacheEntity?
}
