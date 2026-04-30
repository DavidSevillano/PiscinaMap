package com.burixer85.piscinamap.home.presentation

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.components.PiscinaMapBottomBar
import com.burixer85.piscinamap.core.presentation.util.bitmapDescriptorFromVector
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showSearchButton by viewModel.showSearchButton.collectAsStateWithLifecycle()
    val searchTriggeredManually by viewModel.searchTriggeredManually.collectAsStateWithLifecycle()

    var lastPoolCount by remember { mutableStateOf(0) }

    var selectedPool by remember { mutableStateOf<Pool?>(null) }
    var lastSelectedPool by remember { mutableStateOf<Pool?>(null) }

    var poolIconNormal by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var poolIconHighlighted by remember { mutableStateOf<BitmapDescriptor?>(null) }

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    val cameraPositionState = rememberCameraPositionState()

    val focusManager = LocalFocusManager.current

    val properties = remember(locationPermissionState.status.isGranted) {
        MapProperties(
            mapStyleOptions = MapStyleOptions(
                """[
                { "featureType": "poi", "elementType": "labels", "stylers": [{ "visibility": "off" }] },
                { "featureType": "transit", "elementType": "labels", "stylers": [{ "visibility": "off" }] }
            ]"""
            ),
            isMyLocationEnabled = locationPermissionState.status.isGranted
        )
    }

    LaunchedEffect(selectedPool) {
        if (selectedPool != null) {
            lastSelectedPool = selectedPool
        }
    }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (poolIconNormal == null) {
            poolIconNormal = bitmapDescriptorFromVector(
                context,
                R.drawable.poolmark,
                size = 150
            )

            poolIconHighlighted = bitmapDescriptorFromVector(
                context,
                R.drawable.highlighted_poolmark,
                size = 175,
            )
        }

        if (locationPermissionState.status.isGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(userLatLng, 15f)
                        viewModel.fetchPools(it.latitude, it.longitude)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MAP_ERROR", "Error de ubicación: ${e.message}")
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(cameraPositionState.isMoving, cameraPositionState.position.target) {
        viewModel.onMapMoved(cameraPositionState.position.target, cameraPositionState.isMoving)
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            val currentCount = uiState.pools.size

            if (searchTriggeredManually) {
                if (currentCount > lastPoolCount) {
                    val added = currentCount - lastPoolCount

                    val message = if (added == 1) {
                        "¡Se ha encontrado 1 nueva piscina!"
                    } else {
                        "¡Se han encontrado $added nuevas piscinas!"
                    }

                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        context,
                        "No hay nuevas piscinas en esta zona",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                viewModel.clearManualSearchFlag()
            }

            lastPoolCount = currentCount
        }
    }

    LaunchedEffect(uiState.searchLocationResult) {
        uiState.searchLocationResult?.let { latLng ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                durationMs = 1000
            )
            viewModel.onSearchLocationProcessed()
        }
    }

    Scaffold(
        bottomBar = { PiscinaMapBottomBar() }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                properties = properties,
                cameraPositionState = cameraPositionState,
                onMapClick = {
                    selectedPool = null
                    focusManager.clearFocus()
                }
            ) {
                uiState.pools.forEach { pool ->
                    Marker(
                        state = MarkerState(position = LatLng(pool.latitude, pool.longitude)),
                        icon = if (pool.isNew) poolIconHighlighted else poolIconNormal,
                        onClick = {
                            selectedPool = pool
                            viewModel.onMarkerClicked(pool.id)
                            true
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
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color(0xFF1A2F4F)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.searchText,
                        onValueChange = { viewModel.onSearchTextChange(it, context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->

                                if (!focusState.isFocused) {
                                    viewModel.clearPredictions()
                                }
                            },
                        placeholder = { Text("Buscar piscinas o zonas...", color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        },
                        trailingIcon = {
                            if (uiState.searchText.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.onSearchTextChange("", context)
                                    focusManager.clearFocus()
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Borrar",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
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

                    if (uiState.predictions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 250.dp)
                        ) {
                            items(uiState.predictions) { prediction ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onPredictionSelected(prediction, context)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {

                                    Text(
                                        text = prediction.getPrimaryText(null).toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1A2F4F)
                                    )

                                    Text(
                                        text = prediction.getSecondaryText(null).toString(),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE0E6ED))
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedPool != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                lastSelectedPool?.let { pool ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clickable(enabled = false) { },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!pool.photoUrl.isNullOrEmpty()) {
                                val fullPhotoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                                        "?maxwidth=400&photo_reference=${pool.photoUrl}" +
                                        "&key=${BuildConfig.GOOGLEMAPS_KEY}"

                                AsyncImage(
                                    model = fullPhotoUrl,
                                    contentDescription = pool.name,
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(pool.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A2F4F))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFB400), modifier = Modifier.size(16.dp))
                                    Text(" ${pool.rating ?: "N/A"}", fontSize = 14.sp, color = Color.Gray)
                                }
                            }

                            IconButton(onClick = { selectedPool = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }
                    }
                }
            }


            AnimatedVisibility(
                visible = showSearchButton,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 180.dp),
                enter = fadeIn(), exit = fadeOut()
            ) {
                Button(
                    onClick = {
                        val center = cameraPositionState.position.target
                        viewModel.fetchPools(center.latitude, center.longitude, isManual = true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1A2F4F)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(6.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buscar en esta zona", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}