package com.burixer85.piscinamap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.burixer85.piscinamap.home.presentation.HomeScreen
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.GOOGLEMAPS_KEY)
        }

        enableEdgeToEdge()
        setContent {
            PiscinaMapTheme {
                HomeScreen()
            }
        }
    }
}