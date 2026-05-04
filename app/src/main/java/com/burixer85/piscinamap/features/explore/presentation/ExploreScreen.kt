package com.burixer85.piscinamap.features.explore.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.presentation.components.NativeAdCard
import com.burixer85.piscinamap.core.presentation.components.PoolListCard
import com.burixer85.piscinamap.core.presentation.util.HiddenPoolsManager
import com.burixer85.piscinamap.core.presentation.util.LocaleHelper.getString
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay as delaySuspend

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExploreScreen(
    onNavigateToDetail: (String) -> Unit = {},
    bottomPadding: Int = 0,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adLoaded by remember { mutableStateOf(false) }
    var adFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val nativeId = if (BuildConfig.USE_TEST_ADS) {
            "ca-app-pub-3940256099942544/2247696110"
        } else {
            BuildConfig.ADMOB_NATIVE_ID
        }
        Log.d("ADMOB", "Loading native ad with ID: $nativeId, USE_TEST_ADS: ${BuildConfig.USE_TEST_ADS}")

        try {
            val adLoader = AdLoader.Builder(context, nativeId)
                .forNativeAd { ad ->
                    nativeAd = ad
                    adLoaded = true
                    Log.d("ADMOB", "Native ad loaded successfully")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e("ADMOB", "Native ad failed to load: ${adError.message}")
                        adFailed = true
                    }
                    override fun onAdLoaded() {
                        Log.d("ADMOB", "Native ad loaded callback")
                    }
                })
                .build()

            val adRequest = AdRequest.Builder().build()
            adLoader.loadAd(adRequest)

            delaySuspend(8000)
            if (!adLoaded && !adFailed) {
                Log.w("ADMOB", "Native ad timeout after 8 seconds")
                adFailed = true
            }
        } catch (e: Exception) {
            Log.e("ADMOB", "Error loading native ad: ${e.message}")
            adFailed = true
        }
    }

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    var initialLocationObtained by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    LaunchedEffect(locationPermissionState.status.isGranted, uiState.pools.isEmpty()) {
        if (locationPermissionState.status.isGranted && !initialLocationObtained && uiState.pools.isEmpty()) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    viewModel.fetchPools(it.latitude, it.longitude)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            uiState.pools.isEmpty() -> {
                Text(
                    text = stringResource(R.string.no_pools_in_this_area),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.poolmark),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getString(context, R.string.app_name),
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = (16 + bottomPadding).dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val visiblePools = uiState.pools.filter { pool ->
                            !HiddenPoolsManager.isHidden(context, pool.id)
                        }

                        val itemList = buildList {
                            var poolCount = 0
                            visiblePools.forEachIndexed { index, pool ->
                                add(pool)
                                poolCount++
                                if (poolCount == 2 && visiblePools.size > 2) {
                                    add("ad")
                                    poolCount = 0
                                }
                            }
                        }

                        items(itemList.size) { index ->
                            val item = itemList[index]
                            if (item is com.burixer85.piscinamap.core.domain.model.Pool) {
                                PoolListCard(pool = item, onNavigateToDetail = onNavigateToDetail)
                            } else {
                                NativeAdCard(nativeAd = nativeAd)
                            }
                        }

                        item {
                            if (!uiState.hasSearchedMore) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Button(
                                            onClick = { viewModel.fetchMorePools() }
                                        ) {
                                            Text(stringResource(R.string.search_more))
                                        }
                                    }
                                }
                            } else if (uiState.warning != null) {
                                Text(
                                    text = uiState.warning!!,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}