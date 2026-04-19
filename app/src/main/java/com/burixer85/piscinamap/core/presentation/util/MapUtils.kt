package com.burixer85.piscinamap.core.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.scale

fun bitmapDescriptorFromVector(
    context: Context,
    resId: Int,
    size: Int = 120
): BitmapDescriptor {
    val bitmap = BitmapFactory.decodeResource(context.resources, resId)
    val scaledBitmap = bitmap.scale(size, size, false)
    return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
}