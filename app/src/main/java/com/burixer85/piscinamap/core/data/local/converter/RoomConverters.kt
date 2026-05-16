package com.burixer85.piscinamap.core.data.local.converter

import androidx.room.TypeConverter
import com.burixer85.piscinamap.core.domain.model.Review
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun fromReviewList(value: List<Review>): String = json.encodeToString(value)

    @TypeConverter
    fun toReviewList(value: String): List<Review> = json.decodeFromString(value)
}
