package com.burixer85.piscinamap

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient

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

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(
                    OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val request = chain.request().newBuilder()
                                .addHeader("X-Android-Package", BuildConfig.APPLICATION_ID)
                                .addHeader("X-Android-Cert", BuildConfig.SIGNING_CERT_SHA1)
                                .build()
                            chain.proceed(request)
                        }
                        .build()
                )
                .build()
        )
    }
}