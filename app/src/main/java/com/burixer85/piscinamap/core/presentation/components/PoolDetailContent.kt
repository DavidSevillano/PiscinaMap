package com.burixer85.piscinamap.core.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalPhone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.LocaleHelper.getString
import com.burixer85.piscinamap.core.presentation.util.PoolUtils.getGooglePhotoUrl

private fun parseRelativeTime(timeStr: String): Long {
    val lower = timeStr.lowercase()
    val numberRegex = Regex("\\d+")
    val number = numberRegex.find(lower)?.value?.toIntOrNull() ?: 1

    return when {
        lower.contains("now") || lower.contains("just now") -> 0
        lower.contains("minute") || lower.contains("min") -> number * 60L
        lower.contains("hour") || lower.contains("hr") -> number * 3600L
        lower.contains("day") -> number * 86400L
        lower.contains("week") -> number * 604800L
        lower.contains("month") -> number * 2592000L
        lower.contains("year") -> number * 31536000L
        else -> Long.MAX_VALUE
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun PoolDetailContent(
    pool: Pool,
    onCallClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val photoUrls = pool.photoUrls.ifEmpty { listOf(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        val pagerState = rememberPagerState(pageCount = { photoUrls.size })
        val hasMultiplePhotos = photoUrls.size > 1

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photoUrl = photoUrls.getOrNull(page)
                AsyncImage(
                    model = photoUrl?.let { getGooglePhotoUrl(it) }
                        ?: R.drawable.ic_pool_placeholder,
                    contentDescription = getString(context, R.string.pool_photo, pool.name),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (hasMultiplePhotos) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(photoUrls.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.LightGray
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = pool.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pool.rating != null) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = getString(context, R.string.rating_value),
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = pool.rating.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            pool.isOpenNow?.let { isOpen ->
                val openStr = getString(context, R.string.open)
                val closedStr = getString(context, R.string.closed)

                Box(
                    modifier = Modifier
                        .background(
                            if (isOpen) Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else Color(0xFFF44336).copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isOpen) openStr else closedStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOpen) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val uri =
                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${pool.latitude},${pool.longitude}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(getString(context, R.string.get_directions))
            }

            if (pool.formattedPhone != null) {
                OutlinedButton(
                    onClick = { onCallClick(pool.formattedPhone) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalPhone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(getString(context, R.string.call))
                }
            }
        }

        if (pool.openingHours.isNotEmpty() || pool.currentOpeningHours != null) {
            Spacer(modifier = Modifier.height(24.dp))

            var showAllHours by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = getString(context, R.string.schedule),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getString(context, R.string.schedule_today),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val expandStr = getString(context, R.string.expand)
                val collapseStr = getString(context, R.string.collapse)

                if (pool.openingHours.size > 1) {
                    Icon(
                        imageVector = if (showAllHours) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showAllHours) collapseStr else expandStr,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { showAllHours = !showAllHours }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (pool.currentOpeningHours != null) {
                val isOpen = pool.isOpenNow == true
                val openStr = getString(context, R.string.open)
                val closedStr = getString(context, R.string.closed)

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .background(
                            if (isOpen) Color(0xFF4CAF50).copy(alpha = 0.2f)
                            else Color(0xFFF44336).copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOpen) openStr else closedStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOpen) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = pool.currentOpeningHours,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (showAllHours && pool.openingHours.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    pool.openingHours.forEachIndexed { index, hour ->
                        if (index > 0) {
                            Text(
                                text = hour,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        if (pool.services.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = getString(context, R.string.services),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pool.services.forEach { service ->
                    AssistChip(
                        onClick = { },
                        label = { Text(service) }
                    )
                }
            }
        }

        if (pool.reviews.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = getString(context, R.string.reviews),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            var selectedFilter by remember { mutableStateOf("best") }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "best",
                        onClick = { selectedFilter = "best" },
                        label = { Text(getString(context, R.string.best)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "worst",
                        onClick = { selectedFilter = "worst" },
                        label = { Text(getString(context, R.string.worst)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "recent",
                        onClick = { selectedFilter = "recent" },
                        label = { Text(getString(context, R.string.recent)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "oldest",
                        onClick = { selectedFilter = "oldest" },
                        label = { Text(getString(context, R.string.oldest)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val sortedReviews = remember(pool.reviews, selectedFilter) {
                when (selectedFilter) {
                    "recent" -> pool.reviews.sortedBy { parseRelativeTime(it.relativeTimeDescription) }
                    "oldest" -> pool.reviews.sortedByDescending { parseRelativeTime(it.relativeTimeDescription) }
                    "best" -> pool.reviews.sortedByDescending { it.rating }
                    "worst" -> pool.reviews.sortedBy { it.rating }
                    else -> pool.reviews
                }.take(5)
            }

            sortedReviews.forEach { review ->
                ReviewCard(
                    authorName = review.authorName,
                    rating = review.rating,
                    text = review.text,
                    relativeTime = review.relativeTimeDescription,
                    language = review.language,
                    profilePhotoUrl = review.profilePhotoUrl,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
