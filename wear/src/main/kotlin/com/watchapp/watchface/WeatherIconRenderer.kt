package com.watchapp.watchface

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import com.watchapp.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Weather icon: sun + cloud base, with a wind arrow rotated to show blow direction.
 * [windFromDegrees] is meteorological (direction wind comes from, 0° = north).
 */
object WeatherIconRenderer {
    fun draw(
        canvas: Canvas,
        bounds: Rect,
        appContext: android.content.Context,
        windFromDegrees: Int?,
    ) {
        val scale = bounds.layoutScale()
        val left = bounds.scaleX(FaceLayout.weatherIconLeft())
        val top = bounds.scaleY(FaceLayout.WEATHER_ICON_TOP)
        val size = FaceLayout.WEATHER_ICON_SIZE * scale
        val cx = left + size / 2f
        val cy = top + size / 2f
        val r = size * 0.42f

        drawSunAndCloud(canvas, cx, cy, r)

        if (windFromDegrees == null) return
        val arrow = ContextCompat.getDrawable(appContext, R.drawable.ic_wind_arrow) ?: return
        val arrowSize = (size * 0.52f).toInt()
        val rotation = windFromDegrees.toFloat() + 90f
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(rotation)
        arrow.setBounds(-arrowSize / 2, -arrowSize / 2, arrowSize / 2, arrowSize / 2)
        arrow.draw(canvas)
        canvas.restore()
    }

    fun drawAmbient(
        canvas: Canvas,
        bounds: Rect,
        appContext: android.content.Context,
        designLeft: Float,
        designTop: Float,
        designSize: Float,
        windFromDegrees: Int?,
    ) {
        val scale = bounds.layoutScale()
        val left = bounds.scaleX(designLeft)
        val top = bounds.scaleY(designTop)
        val size = designSize * scale
        val cx = left + size / 2f
        val cy = top + size / 2f
        val r = size * 0.42f

        drawSunAndCloud(canvas, cx, cy, r)

        if (windFromDegrees == null) return
        val arrow = ContextCompat.getDrawable(appContext, R.drawable.ic_wind_arrow) ?: return
        val arrowSize = (size * 0.48f).toInt()
        val rotation = windFromDegrees.toFloat() + 90f
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(rotation)
        arrow.setBounds(-arrowSize / 2, -arrowSize / 2, arrowSize / 2, arrowSize / 2)
        arrow.draw(canvas)
        canvas.restore()
    }

    private fun drawSunAndCloud(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F4C430") }
        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F4C430")
            strokeWidth = r * 0.12f
            strokeCap = Paint.Cap.ROUND
        }
        val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B0B8C0") }

        val sunCx = cx - r * 0.22f
        val sunCy = cy - r * 0.18f
        canvas.drawCircle(sunCx, sunCy, r * 0.38f, sunPaint)
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0)
            val inner = r * 0.42f
            val outer = r * 0.62f
            canvas.drawLine(
                sunCx + inner * cos(angle).toFloat(),
                sunCy + inner * sin(angle).toFloat(),
                sunCx + outer * cos(angle).toFloat(),
                sunCy + outer * sin(angle).toFloat(),
                rayPaint,
            )
        }

        val cloudCy = cy + r * 0.28f
        canvas.drawCircle(cx - r * 0.35f, cloudCy, r * 0.32f, cloudPaint)
        canvas.drawCircle(cx, cloudCy - r * 0.08f, r * 0.38f, cloudPaint)
        canvas.drawCircle(cx + r * 0.35f, cloudCy, r * 0.30f, cloudPaint)
    }
}
