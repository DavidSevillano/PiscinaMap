package com.burixer85.piscinamap

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PiscinaMapApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val testDeviceId = BuildConfig.TEST_DEVICE_ID
        if (testDeviceId.isNotEmpty()) {
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(testDeviceId))
                .build()
            MobileAds.setRequestConfiguration(configuration)
        }

        MobileAds.initialize(this) {}

        if (BuildConfig.GOOGLEMAPS_KEY.isNotEmpty()) {
            Places.initialize(this, BuildConfig.GOOGLEMAPS_KEY)
        }
    }
}