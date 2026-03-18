package com.noxis.os.util

import android.content.Context
import android.util.TypedValue

fun Context.dpToPx(dp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

fun Context.dpToPx(dp: Int): Int = dpToPx(dp.toFloat()).toInt()
fun Context.pxToDp(px: Float): Float = px / resources.displayMetrics.density
