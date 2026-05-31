package com.apoorvdarshan.ai.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/**
 * Renders the iOS-style pink-gradient progress ring into a Bitmap so the
 * Glance widget can draw it via Image(provider = ImageProvider(bitmap)).
 *
 * Glance has no Canvas / arc primitives, so the ring is rasterized in the
 * widget update path. Mirrors iOS `WidgetPalette.calorieGradient`
 * (#FF375F → #FF6B8A, top-leading → bottom-trailing) with a 15%-alpha track
 * and a round stroke cap matching `lineCap: .round`.
 */
fun ringBitmap(sizePx: Int, progress: Float, strokeWidthPx: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val pad = strokeWidthPx / 2f
    val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)

    val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = AndroidColor.argb(38, 0xFF, 0x37, 0x5F)
    }
    canvas.drawArc(rect, 0f, 360f, false, track)

    val sweep = progress.coerceIn(0f, 1f) * 360f
    if (sweep > 0f) {
        val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                AndroidColor.rgb(0xFF, 0x37, 0x5F),
                AndroidColor.rgb(0xFF, 0x6B, 0x8A),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawArc(rect, -90f, sweep, false, fg)
    }

    return bitmap
}

/**
 * Capsule pill bar — pink track + pink-gradient fill clipped to `progress`.
 * Used for the Protein/Carbs/Fat rows in the medium widget so they look like
 * the iOS Capsule + Capsule(progress) pair instead of Glance's stock
 * LinearProgressIndicator.
 */
fun capsuleBitmap(widthPx: Int, heightPx: Int, progress: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = heightPx / 2f
    val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())

    val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.argb(38, 0xFF, 0x37, 0x5F)
    }
    canvas.drawRoundRect(rect, radius, radius, track)

    val clamped = progress.coerceIn(0f, 1f)
    if (clamped > 0f) {
        val fillW = (widthPx * clamped).coerceAtLeast(heightPx.toFloat())
        val fillRect = RectF(0f, 0f, fillW, heightPx.toFloat())
        val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, widthPx.toFloat(), heightPx.toFloat(),
                AndroidColor.rgb(0xFF, 0x37, 0x5F),
                AndroidColor.rgb(0xFF, 0x6B, 0x8A),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(fillRect, radius, radius, fg)
    }

    return bitmap
}
