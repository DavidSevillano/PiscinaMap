package com.burixer85.piscinamap.home.presentation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.presentation.components.PiscinaMapBottomBar
import com.burixer85.piscinamap.core.presentation.util.bitmapDescriptorFromVector
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
){
    val context = LocalContext.current

    var poolIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.3891, -5.9845), 14f)
    }

    val mapStyleOptions = remember {
        MapStyleOptions("""
            [
              { "featureType": "poi", "elementType": "labels", "stylers": [{ "visibility": "off" }] },
              { "featureType": "transit", "elementType": "labels", "stylers": [{ "visibility": "off" }] }
            ]
        """.trimIndent())
    }

    val properties by remember {
        mutableStateOf(MapProperties(mapStyleOptions = mapStyleOptions))
    }

    LaunchedEffect(Unit) {
        try {
            poolIcon = bitmapDescriptorFromVector(context, R.drawable.poolmark, size = 150)
        } catch (e: Exception) {
            Log.e("MAP_ERROR", "Error cargando el icono: ${e.message}")
        }
        viewModel.fetchPools(37.3891, -5.9845)
    }

    Scaffold(
        bottomBar = { PiscinaMapBottomBar() }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                properties = properties,
                cameraPositionState = cameraPositionState,
            ) {
                uiState.pools.forEach { pool ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(pool.latitude, pool.longitude)
                        ),
                        title = pool.name,
                        icon = poolIcon,
                        snippet = "Valoración: ${pool.rating ?: "N/A"}",
                        onClick = {
                            false
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uiState.searchText,
                            onValueChange = { viewModel.onSearchTextChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Buscar piscinas o zonas...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                )
            }
        }
    }
}