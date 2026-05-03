package com.burixer85.piscinamap

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.burixer85.piscinamap.navigation.PiscinaMapNavGraph
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val adIntervalMs = 3 * 60 * 1000L // 3 minutos
    private var adShown = false

    private val showAdRunnable = object : Runnable {
        override fun run() {
            loadAndShowAd()
            handler.postDelayed(this, adIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            PiscinaMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF1EEE8)
                ) {
                    PiscinaMapNavGraph()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (!adShown) {
            handler.postDelayed(showAdRunnable, 10000)
            adShown = true
        }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(showAdRunnable)
    }

    private fun loadAndShowAd() {
        InterstitialAd.load(
            this,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.show(this@MainActivity)
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {}
            }
        )
    }
}