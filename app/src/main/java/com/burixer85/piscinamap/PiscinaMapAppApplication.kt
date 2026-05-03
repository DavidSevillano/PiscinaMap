package com.burixer85.piscinamap

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
    }
}