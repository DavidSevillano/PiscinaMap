package com.burixer85.piscinamap.core.presentation.util

import com.burixer85.piscinamap.BuildConfig

object PoolUtils {
    fun getGooglePhotoUrl(reference: String?): String? {
        if (reference.isNullOrEmpty()) return null
        return "https://maps.googleapis.com/maps/api/place/photo" +
                "?maxwidth=400&photo_reference=$reference" +
                "&key=${BuildConfig.GOOGLEMAPS_KEY}"
    }

    fun getCurrentDayIndex(): Int {
        val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            java.util.Calendar.SATURDAY -> 5
            java.util.Calendar.SUNDAY -> 6
            else -> 0
        }
    }
}