package com.burixer85.piscinamap.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.domain.model.Review

@Entity(tableName = "pool_detail_cache")
data class PoolDetailCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "place_id")
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val rating: Float?,
    @ColumnInfo(name = "is_open_now") val isOpenNow: Boolean?,
    @ColumnInfo(name = "photo_urls") val photoUrls: List<String>,
    @ColumnInfo(name = "opening_hours") val openingHours: List<String>,
    @ColumnInfo(name = "current_opening_hours") val currentOpeningHours: String?,
    val services: List<String>,
    val reviews: List<Review>,
    @ColumnInfo(name = "formatted_phone") val formattedPhone: String?,
    @ColumnInfo(name = "cached_at") val cachedAt: Long
)

fun PoolDetailCacheEntity.toDomain(): Pool = Pool(
    id = placeId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    address = address,
    rating = rating,
    isOpenNow = isOpenNow,
    photoUrls = photoUrls,
    openingHours = openingHours,
    currentOpeningHours = currentOpeningHours,
    services = services,
    reviews = reviews,
    formattedPhone = formattedPhone
)

fun Pool.toDetailCacheEntity(): PoolDetailCacheEntity = PoolDetailCacheEntity(
    placeId = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    address = address,
    rating = rating,
    isOpenNow = isOpenNow,
    photoUrls = photoUrls,
    openingHours = openingHours,
    currentOpeningHours = currentOpeningHours,
    services = services,
    reviews = reviews,
    formattedPhone = formattedPhone,
    cachedAt = System.currentTimeMillis()
)
