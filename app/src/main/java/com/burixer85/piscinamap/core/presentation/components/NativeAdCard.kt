package com.burixer85.piscinamap.core.presentation.components

import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    nativeAd: NativeAd? = null
) {
    val context = LocalContext.current

    if (nativeAd != null) {
        AndroidView(
            factory = { ctx ->
                val adView = NativeAdView(ctx)

                val cardView = CardView(ctx).apply {
                    setCardBackgroundColor(android.graphics.Color.WHITE)
                    radius = 40f
                    cardElevation = 16f
                    useCompatPadding = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                val container = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    setPadding(32, 24, 32, 24)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                val mediaView = com.google.android.gms.ads.nativead.MediaView(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(120, 120)
                }

                val textContainer = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        marginStart = 32
                    }
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val headlineView = android.widget.TextView(ctx).apply {
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(0xFF1A2F4F.toInt())
                    maxLines = 1
                }

                val bodyView = android.widget.TextView(ctx).apply {
                    textSize = 14f
                    setTextColor(0xFF666666.toInt())
                    maxLines = 1
                    setPadding(0, 8, 0, 0)
                }

                val ctaButton = android.widget.Button(ctx).apply {
                    text = nativeAd.callToAction ?: "Ver más"
                    setTextColor(0xFF4CAF50.toInt())
                    textSize = 11f
                    setPadding(0, 8, 0, 0)
                    background = null
                }

                textContainer.addView(headlineView)
                textContainer.addView(bodyView)
                textContainer.addView(ctaButton)

                container.addView(mediaView)
                container.addView(textContainer)

                cardView.addView(container)
                adView.addView(cardView)

                adView.headlineView = headlineView
                adView.bodyView = bodyView
                adView.callToActionView = ctaButton
                adView.mediaView = mediaView

                (adView.headlineView as? android.widget.TextView)?.text = nativeAd.headline
                (adView.bodyView as? android.widget.TextView)?.text = nativeAd.body
                (adView.callToActionView as? android.widget.Button)?.text = nativeAd.callToAction

                adView.mediaView?.setMediaContent(nativeAd.mediaContent)

                adView.setNativeAd(nativeAd)
                adView
            },
            modifier = modifier.fillMaxWidth(),
            onRelease = { it.destroy() }
        )
    } else {
        NativeAdPlaceholder(modifier = modifier)
    }
}

@Composable
fun NativeAdPlaceholder(
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    )
                }
            }
        }
    }
}