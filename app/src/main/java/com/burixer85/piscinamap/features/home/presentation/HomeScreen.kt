package com.burixer85.piscinamap.features.home.presentation

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.domain.model.Pool
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
    onNavigateToDetail: (String) -> Unit,
    bottomPadding: Int = 0
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val cameraPositionState = rememberCameraPositionState()
    val focusManager = LocalFocusManager.current

    var selectedPool by remember { mutableStateOf<Pool?>(null) }
    var lastSelectedPool by remember { mutableStateOf<Pool?>(null) }
    var poolIconNormal by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var poolIconHighlighted by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var poolIconHidden by remember { mutableStateOf<BitmapDescriptor?>(null) }
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.AnimateToLocation -> {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            event.latLng,
                            15f
                        ), 1000
                    )
                }

                is HomeEvent.ShowToast -> {
                    snackbarMessage = if (event.newPoolsCount > 0) {
                        context.getString(R.string.new_pools_found, event.newPoolsCount)
                    } else {
                        context.getString(R.string.no_new_pools)
                    }
                }
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            kotlinx.coroutines.delay(3000)
            snackbarMessage = null
        }
    }

    LaunchedEffect(selectedPool) {
        if (selectedPool != null) lastSelectedPool = selectedPool
    }

    @SuppressLint("MissingPermission")
    LaunchedEffect(Unit) {
        if (poolIconNormal == null) {
            poolIconNormal = bitmapDescriptorFromVector(context, R.drawable.poolmark, size = 150)
            poolIconHighlighted =
                bitmapDescriptorFromVector(context, R.drawable.highlighted_poolmark, size = 175)
            poolIconHidden =
                bitmapDescriptorFromVector(context, R.drawable.poolmark_hidden, size = 150)
        }

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

    Box(
        modifier = Modifier.fillMaxSize()
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
                    val isSelected by remember(selectedPool, pool.id, pool.isHidden) {
                        derivedStateOf { selectedPool?.id == pool.id }
                    }

                    val icon = when {
                        isSelected -> poolIconSelected
                        pool.isHidden -> poolIconHidden
                        pool.isNew -> poolIconHighlighted
                        else -> poolIconNormal
                    }

                    Marker(
                        state = MarkerState(position = LatLng(pool.latitude, pool.longitude)),
                        icon = icon,
                        onClick = {
                            selectedPool = pool
                            viewModel.onMarkerClicked(pool.id)
                            true
                        },

                        )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surface)
            )
            PoolSearchBar(
                searchText = uiState.searchText,
                predictions = uiState.predictions,
                onSearchTextChange = { viewModel.onSearchTextChange(it, context) },
                onPredictionClick = { prediction ->
                    focusManager.clearFocus()
                    selectedPool = null
                    viewModel.onPredictionSelected(prediction, context)
                },
                onClearSearch = {
                    viewModel.onSearchTextChange("", context)
                    focusManager.clearFocus()
                },
                onFocusChanged = { isFocused ->
                    if (!isFocused) viewModel.clearPredictions()
                },
            )

            AnimatedVisibility(
                visible = uiState.showSearchButton,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SearchAreaButton(
                        onClick = {
                            val center = cameraPositionState.position.target
                            viewModel.fetchPools(
                                center.latitude,
                                center.longitude,
                                context = context,
                                isManual = true
                            )
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedPool != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = bottomPadding.dp
                ),
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


        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
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

        if (snackbarMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = (-220).dp)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.highlighted_poolmark),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = snackbarMessage ?: "",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}