package com.burixer85.piscinamap.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.burixer85.piscinamap.core.domain.model.Pool

@Entity(tableName = "pool_cache")
data class PoolCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "place_id")
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val rating: Float?,
    @ColumnInfo(name = "is_open_now") val isOpenNow: Boolean?,
    @ColumnInfo(name = "cached_at") val cachedAt: Long
)

fun PoolCacheEntity.toDomain(): Pool = Pool(
    id = placeId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    address = address,
    rating = rating,
    isOpenNow = isOpenNow
)

fun Pool.toCacheEntity(): PoolCacheEntity = PoolCacheEntity(
    placeId = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    address = address,
    rating = rating,
    isOpenNow = isOpenNow,
    cachedAt = System.currentTimeMillis()
)
