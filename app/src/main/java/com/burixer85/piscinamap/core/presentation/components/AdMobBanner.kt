package com.burixer85.piscinamap.core.presentation.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.burixer85.piscinamap.BuildConfig
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier
) {
    var adViewRef by remember { mutableStateOf<AdManagerAdView?>(null) }

    DisposableEffect(Unit) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val reloadRunnable = object : Runnable {
            override fun run() {
                adViewRef?.loadAd(AdRequest.Builder().build())
                handler.postDelayed(this, 30000)
            }
        }
        handler.postDelayed(reloadRunnable, 30000)
        onDispose {
            handler.removeCallbacks(reloadRunnable)
        }
    }

    AndroidView(
        factory = { ctx ->
            val bannerId = if (BuildConfig.USE_TEST_ADS) "ca-app-pub-3940256099942544/6300978111" else BuildConfig.ADMOB_BANNER_ID
            AdManagerAdView(ctx).apply {
                setAdSize(AdSize.SMART_BANNER)
                adUnitId = bannerId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adListener = object : AdListener() {
                    override fun onAdLoaded() {}
                    override fun onAdFailedToLoad(error: LoadAdError) {}
                }
                loadAd(AdRequest.Builder().build())
                adViewRef = this
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}