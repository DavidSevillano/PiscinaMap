package com.burixer85.piscinamap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.burixer85.piscinamap.navigation.PiscinaMapNavGraph
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
}