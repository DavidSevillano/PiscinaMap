package com.burixer85.piscinamap

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.burixer85.piscinamap.navigation.PiscinaMapNavGraph
import com.burixer85.piscinamap.core.presentation.components.UpdateAvailableDialog
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

    private var showUpdateDialog by mutableStateOf(false)
    private val latestAppVersion = "1.0.2" // Change to higher version to show update dialog
    private val showUpdateCheckEnabled = false // Set to true to enable update check

    private val showAdRunnable = object : Runnable {
        override fun run() {
            loadAndShowAd()
            handler.postDelayed(this, adIntervalMs)
        }
    }

    private fun openPlayStore() {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.burixer85.piscinamap"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.burixer85.piscinamap"))
        try {
            startActivity(marketIntent)
        } catch (e: Exception) {
            startActivity(webIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                "alpha",
                1f,
                0f
            )
            fadeOut.duration = 500
            fadeOut.interpolator = AccelerateDecelerateInterpolator()
            fadeOut.start()
        }

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = 0xFFFAFAFA.toInt()
        window.navigationBarColor = 0x00000000

        setContent {
            val currentVersion = BuildConfig.VERSION_NAME
            PiscinaMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF1EEE8)
                ) {
                    Box {
                        PiscinaMapNavGraph()
                        if ((showUpdateDialog || (showUpdateCheckEnabled && currentVersion != latestAppVersion))) {
                            UpdateAvailableDialog(
                                onUpdate = {
                                    showUpdateDialog = false
                                    openPlayStore()
                                },
                                onDismiss = { showUpdateDialog = false },
                                versionName = latestAppVersion
                            )
                        }
                    }
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
        val interstitialId = if (BuildConfig.USE_TEST_ADS) "ca-app-pub-3940256099942544/5224354917" else BuildConfig.ADMOB_INTERSTITIAL_ID
        InterstitialAd.load(
            this,
            interstitialId,
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