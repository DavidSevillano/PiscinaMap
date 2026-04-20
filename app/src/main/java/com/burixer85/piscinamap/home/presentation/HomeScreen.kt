package com.burixer85.piscinamap.home.presentation

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.presentation.components.PiscinaMapBottomBar
import com.burixer85.piscinamap.core.presentation.util.bitmapDescriptorFromVector
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
){
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var poolIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    val cameraPositionState = rememberCameraPositionState()

    val properties = remember(locationPermissionState.status.isGranted) {
        MapProperties(
            mapStyleOptions = MapStyleOptions("""[
                { "featureType": "poi", "elementType": "labels", "stylers": [{ "visibility": "off" }] },
                { "featureType": "transit", "elementType": "labels", "stylers": [{ "visibility": "off" }] }
            ]"""),
            isMyLocationEnabled = locationPermissionState.status.isGranted
        )
    }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (poolIcon == null) {
            poolIcon = bitmapDescriptorFromVector(context, R.drawable.poolmark, size = 150)
        }

        if (locationPermissionState.status.isGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
                        viewModel.fetchPools(it.latitude, it.longitude)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MAP_ERROR", "No se pudo obtener la ubicación: ${e.message}")
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
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

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.poolmark),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PiscinaMap",
                            style = androidx.compose.ui.text.TextStyle(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color(0xFF1A2F4F)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.searchText,
                        onValueChange = { viewModel.onSearchTextChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar piscinas o zonas...", color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF7F8FA),
                            unfocusedContainerColor = Color(0xFFF7F8FA),
                            disabledContainerColor = Color(0xFFF7F8FA),
                            focusedBorderColor = Color(0xFFE0E6ED),
                            unfocusedBorderColor = Color(0xFFE0E6ED),
                            disabledBorderColor = Color(0xFFE0E6ED),
                            focusedTextColor = Color(0xFF1D293F),
                            unfocusedTextColor = Color(0xFF1D293F),
                            cursorColor = Color(0xFF1A2F4F)
                        )
                    )
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