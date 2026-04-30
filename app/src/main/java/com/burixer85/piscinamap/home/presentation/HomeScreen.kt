package com.burixer85.piscinamap.home.presentation

import android.annotation.SuppressLint
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
import com.burixer85.piscinamap.core.presentation.components.PoolDetailCard
import com.burixer85.piscinamap.core.presentation.components.PoolSearchBar
import com.burixer85.piscinamap.core.presentation.components.SearchAreaButton
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

    val cameraPositionState = rememberCameraPositionState()
    val focusManager = LocalFocusManager.current

    var selectedPool by remember { mutableStateOf<Pool?>(null) }
    var lastSelectedPool by remember { mutableStateOf<Pool?>(null) }
    var poolIconNormal by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var poolIconHighlighted by remember { mutableStateOf<BitmapDescriptor?>(null) }
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.AnimateToLocation -> {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(event.latLng, 15f), 1000)
                }
                is HomeEvent.ShowToast -> {
                    Toast.makeText(context, event.message, if (event.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(selectedPool) {
        if (selectedPool != null) lastSelectedPool = selectedPool
    }

    @SuppressLint("MissingPermission")
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (poolIconNormal == null) {
            poolIconNormal = bitmapDescriptorFromVector(context, R.drawable.poolmark, size = 150)
            poolIconHighlighted = bitmapDescriptorFromVector(context, R.drawable.highlighted_poolmark, size = 175)
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
                Log.e("MAP_ERROR", "Error de ubicación: ${e.message}")
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(cameraPositionState.isMoving, cameraPositionState.position.target) {
        viewModel.onMapMoved(cameraPositionState.position.target, cameraPositionState.isMoving)
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
                properties = remember(locationPermissionState.status.isGranted) {
                    MapProperties(
                        mapStyleOptions = MapStyleOptions("[{ \"featureType\": \"poi\", \"stylers\": [{ \"visibility\": \"off\" }] }]"),
                        isMyLocationEnabled = locationPermissionState.status.isGranted
                    )
                },
                cameraPositionState = cameraPositionState,
                onMapClick = { selectedPool = null; focusManager.clearFocus() }
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

            PoolSearchBar(
                searchText = uiState.searchText,
                predictions = uiState.predictions,
                onSearchTextChange = { viewModel.onSearchTextChange(it, context) },
                onPredictionClick = { viewModel.onPredictionSelected(it, context) },
                onClearSearch = {
                    viewModel.onSearchTextChange("", context)
                    focusManager.clearFocus()
                },
                onFocusChanged = { isFocused ->
                    if (!isFocused) viewModel.clearPredictions()
                }
            )


            AnimatedVisibility(
                visible = selectedPool != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                lastSelectedPool?.let { pool ->
                    PoolDetailCard(
                        pool = pool,
                        onClose = { selectedPool = null }
                    )
                }
            }


            AnimatedVisibility(
                visible = uiState.showSearchButton,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 180.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SearchAreaButton(
                    onClick = {
                        val center = cameraPositionState.position.target
                        viewModel.fetchPools(center.latitude, center.longitude, isManual = true)
                    }
                )
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