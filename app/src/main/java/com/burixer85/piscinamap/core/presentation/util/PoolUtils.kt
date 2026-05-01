package com.burixer85.piscinamap.core.presentation.util

import com.burixer85.piscinamap.BuildConfig

object PoolUtils {
    fun getGooglePhotoUrl(reference: String?): String? {
        if (reference.isNullOrEmpty()) return null
        return "https://maps.googleapis.com/maps/api/place/photo" +
                "?maxwidth=400&photo_reference=$reference" +
                "&key=${BuildConfig.GOOGLEMAPS_KEY}"
    }
}