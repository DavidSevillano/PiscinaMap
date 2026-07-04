package com.burixer85.piscinamap

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.burixer85.piscinamap.core.presentation.components.UpdateAvailableDialog
import com.burixer85.piscinamap.navigation.PiscinaMapNavGraph
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val adIntervalMs = 3 * 60 * 1000L
    private var lastAdShownAt = 0L

    private var showUpdateDialog by mutableStateOf(false)

    private fun checkForUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                showUpdateDialog = true
            }
        }
    }

    private fun openPlayStore() {
        val marketIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.burixer85.piscinamap"))
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.burixer85.piscinamap")
        )
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

        WindowCompat.getInsetsController(window, window.decorView).let { ctrl ->
            ctrl.hide(WindowInsetsCompat.Type.systemBars())
            ctrl.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        checkForUpdate()

        setContent {
            PiscinaMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box {
                        PiscinaMapNavGraph()
                        if (showUpdateDialog) {
                            UpdateAvailableDialog(
                                onUpdate = {
                                    showUpdateDialog = false
                                    openPlayStore()
                                },
                                onDismiss = { showUpdateDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }

    fun onPoolDetailOpened() {
        if (BuildConfig.DISABLE_ADS) return
        val now = System.currentTimeMillis()
        if (lastAdShownAt == 0L || now - lastAdShownAt >= adIntervalMs) {
            lastAdShownAt = now
            loadAndShowAd()
        }
    }

    private fun loadAndShowAd() {
        val interstitialId =
            if (BuildConfig.USE_TEST_ADS) "ca-app-pub-3940256099942544/5224354917" else BuildConfig.ADMOB_INTERSTITIAL_ID
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