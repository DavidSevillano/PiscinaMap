package com.burixer85.piscinamap.core.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.scale

@RequiresApi(Build.VERSION_CODES.O)
fun bitmapDescriptorFromVector(
    context: android.content.Context,
    vectorResId: Int,
    size: Int,
    tint: Color? = null
): BitmapDescriptor? {
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, vectorResId) ?: return null

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    drawable.setBounds(0, 0, canvas.width, canvas.height)

    tint?.let {
        DrawableCompat.setTint(drawable, it.toArgb())
    }

    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}