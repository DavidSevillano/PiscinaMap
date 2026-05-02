package com.burixer85.piscinamap.features.home.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
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
        Manifest.permission.ACCESS_FINE_LOCATION
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

        if (uiState.pools.isNotEmpty()) return@LaunchedEffect

        if (locationPermissionState.status.isGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)

                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
                    viewModel.fetchPools(it.latitude, it.longitude, context = context)
                }
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

            val mapProperties = remember(locationPermissionState.status.isGranted) {
                MapProperties(
                    mapStyleOptions = MapStyleOptions("[{ \"featureType\": \"poi\", \"stylers\": [{ \"visibility\": \"off\" }] }]"),
                    isMyLocationEnabled = locationPermissionState.status.isGranted
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    properties = mapProperties,
                    cameraPositionState = cameraPositionState,
                    onMapClick = { selectedPool = null; focusManager.clearFocus() }
                ) {
                    val poolIconSelected =
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)

                    uiState.pools.forEach { pool ->
                        val isSelected by remember(selectedPool, pool.id) {
                            derivedStateOf { selectedPool?.id == pool.id }
                        }

                        Marker(
                            state = MarkerState(position = LatLng(pool.latitude, pool.longitude)),
                            icon = when {
                                isSelected -> poolIconSelected
                                pool.isNew -> poolIconHighlighted
                                else -> poolIconNormal
                            },
                            onClick = {
                                selectedPool = pool
                                viewModel.onMarkerClicked(pool.id)
                                true
                            },

                            )
                    }
                }
            }

            PoolSearchBar(
                searchText = uiState.searchText,
                predictions = uiState.predictions,
                onSearchTextChange = { viewModel.onSearchTextChange(it, context) },
                onPredictionClick = { prediction ->
                    focusManager.clearFocus()
                    selectedPool = null
                    viewModel.onPredictionSelected(prediction, context) },
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
                        onClose = { selectedPool = null },
                        onNavigateToDetail = { id ->
                            focusManager.clearFocus()
                            onNavigateToDetail(id)
                        }
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
                        viewModel.fetchPools(center.latitude, center.longitude, context = context, isManual = true)
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