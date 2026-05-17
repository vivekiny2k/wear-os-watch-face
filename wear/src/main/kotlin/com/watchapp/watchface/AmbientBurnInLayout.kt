package com.watchapp.watchface

import android.graphics.Paint
import android.graphics.Rect
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ambient burn-in: two rigid units on the inner orbit.
 *
 * - **Time box** (3 rows): HH:mm, TZ1, TZ2 — centered at **current hour** on the 12h dial.
 * - **Weather box**: temp, wind, icon — centered at **hour + 6** (opposite side).
 *
 * Dial center C = (225, 225), radius R = [ORBIT_RADIUS]. Hour h (0–11):
 * ```
 * θ_h = h × 30°   (0° = 12 o'clock, clockwise)
 * anchor(θ) = (Cx + R·sin(θ), Cy − R·cos(θ))
 * ```
 *
 * Clock row sits on the orbit ring; TZ1/TZ2 stack downward on screen (+design Y).
 * Weather: temp on top, wind below; icon beside the text column along the clockwise tangent.
 */
object AmbientBurnInLayout {
    private const val CHORD_INSET = 28f
    /** ~70% of dial radius — keeps clusters near the bezel, not hugging the center. */
    const val ORBIT_RADIUS = 158f

    const val CLOCK_TZ1_GAP = 3f
    const val TZ1_TZ2_GAP = 1f
    const val WEATHER_TEXT_GAP = 6f
    const val WEATHER_ICON_SIZE = 52f
    const val WEATHER_ICON_TEXT_GAP = 8f

    const val AMBIENT_TIME_BOX_H = 80f

    data class OrbitAnchor(
        val centerX: Float,
        val centerY: Float,
        val inwardX: Float,
        val inwardY: Float,
        val outwardX: Float,
        val outwardY: Float,
        val tangentX: Float,
        val tangentY: Float,
        val textAlign: Paint.Align,
    )

    data class TimeBox(
        val anchor: OrbitAnchor,
        val drawX: Float,
        val clockBaseline: Float,
        val tz1Baseline: Float,
        val tz2Baseline: Float,
        val maxTextWidthDesign: Float,
    )

    data class WeatherBox(
        val anchor: OrbitAnchor,
        val drawX: Float,
        val tempBaseline: Float,
        val windBaseline: Float,
        val iconLeft: Float,
        val iconTop: Float,
        val tempMaxWidthDesign: Float,
        val windMaxWidthDesign: Float,
    )

    fun currentHour12(zonedDateTime: ZonedDateTime): Int =
        zonedDateTime.hour % 12

    fun hourDegrees(hour12: Int): Float = hour12 * 30f

    fun weatherHour12(hour12: Int): Int = (hour12 + 6) % 12

    fun orbitAnchor(degrees: Float): OrbitAnchor {
        val cx = FaceLayout.DESIGN_CENTER
        val cy = FaceLayout.DESIGN_CENTER
        val rad = Math.toRadians(degrees.toDouble())
        val sinA = sin(rad).toFloat()
        val cosA = cos(rad).toFloat()

        val centerX = cx + ORBIT_RADIUS * sinA
        val centerY = cy - ORBIT_RADIUS * cosA

        var outwardX = centerX - cx
        var outwardY = centerY - cy
        val len = sqrt(outwardX * outwardX + outwardY * outwardY)
        if (len > 0f) {
            outwardX /= len
            outwardY /= len
        }
        val inwardX = -outwardX
        val inwardY = -outwardY
        // Clockwise tangent (viewed from above dial)
        val tangentX = outwardY
        val tangentY = -outwardX

        return OrbitAnchor(
            centerX = centerX,
            centerY = centerY,
            inwardX = inwardX,
            inwardY = inwardY,
            outwardX = outwardX,
            outwardY = outwardY,
            tangentX = tangentX,
            tangentY = tangentY,
            textAlign = textAlignForDegrees(degrees),
        )
    }

    private fun textAlignForDegrees(degrees: Float): Paint.Align {
        val d = ((degrees % 360f) + 360f) % 360f
        return when {
            d < 30f || d >= 330f -> Paint.Align.CENTER
            d < 150f -> Paint.Align.RIGHT
            d < 210f -> Paint.Align.CENTER
            else -> Paint.Align.LEFT
        }
    }

    fun clampDrawX(anchor: OrbitAnchor): Float {
        val y = anchor.centerY
        return when (anchor.textAlign) {
            Paint.Align.LEFT -> anchor.centerX.coerceAtLeast(FaceLayout.chordLeftXAtY(y) + CHORD_INSET)
            Paint.Align.RIGHT -> anchor.centerX.coerceAtMost(FaceLayout.chordRightXAtY(y) - CHORD_INSET)
            Paint.Align.CENTER -> anchor.centerX.coerceIn(
                FaceLayout.chordLeftXAtY(y) + CHORD_INSET,
                FaceLayout.chordRightXAtY(y) - CHORD_INSET,
            )
            else -> anchor.centerX
        }
    }

    private fun offset(
        x: Float,
        y: Float,
        dx: Float,
        dy: Float,
        distance: Float,
    ): Pair<Float, Float> = x + dx * distance to y + dy * distance

    /**
     * Three-row time box: HH:mm on the orbit ring; TZ1/TZ2 below on screen (+design Y).
     */
    fun timeBox(
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        clockPaint: Paint,
        tz1Paint: Paint,
        tz2Paint: Paint,
    ): TimeBox {
        val hour12 = currentHour12(zonedDateTime)
        val anchor = orbitAnchor(hourDegrees(hour12))
        val drawX = clampDrawX(anchor)

        val clockInk = clockPaint.inkHeightDesign(bounds)
        val tz1Ink = tz1Paint.inkHeightDesign(bounds)
        val tz2Ink = tz2Paint.inkHeightDesign(bounds)

        val clockCy = anchor.centerY
        val tz1Cy = clockCy + clockInk / 2f + CLOCK_TZ1_GAP + tz1Ink / 2f
        val tz2Cy = tz1Cy + tz1Ink / 2f + TZ1_TZ2_GAP + tz2Ink / 2f

        val tzMidY = (tz1Cy + tz2Cy) / 2f
        val maxW = maxTextWidth(anchor, drawX, tzMidY)

        return TimeBox(
            anchor = anchor,
            drawX = drawX,
            clockBaseline = bounds.baselineAtDesignCenter(clockCy, clockPaint),
            tz1Baseline = bounds.baselineAtDesignCenter(tz1Cy, tz1Paint),
            tz2Baseline = bounds.baselineAtDesignCenter(tz2Cy, tz2Paint),
            maxTextWidthDesign = maxW,
        )
    }

    /**
     * Weather unit: temp on orbit ring, wind below; icon beside text along tangent.
     */
    fun weatherBox(
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        tempPaint: Paint,
        windPaint: Paint,
    ): WeatherBox {
        val hour12 = currentHour12(zonedDateTime)
        val anchor = orbitAnchor(hourDegrees(weatherHour12(hour12)))
        val drawX = clampDrawX(anchor)

        val tempInk = tempPaint.inkHeightDesign(bounds)
        val windInk = windPaint.inkHeightDesign(bounds)

        val tempCy = anchor.centerY
        val windCy = tempCy + tempInk / 2f + WEATHER_TEXT_GAP + windInk / 2f

        val textMidY = (tempCy + windCy) / 2f
        val textMaxW = maxTextWidth(anchor, drawX, textMidY)

        val iconOffset = (textMaxW / 2f + WEATHER_ICON_TEXT_GAP + WEATHER_ICON_SIZE / 2f)
            .coerceAtMost(ORBIT_RADIUS * 0.45f)
        val (iconCx, iconCy) = offset(
            drawX,
            textMidY,
            anchor.tangentX,
            anchor.tangentY,
            iconOffset,
        )

        return WeatherBox(
            anchor = anchor,
            drawX = drawX,
            tempBaseline = bounds.baselineAtDesignCenter(tempCy, tempPaint),
            windBaseline = bounds.baselineAtDesignCenter(windCy, windPaint),
            iconLeft = iconCx - WEATHER_ICON_SIZE / 2f,
            iconTop = iconCy - WEATHER_ICON_SIZE / 2f,
            tempMaxWidthDesign = textMaxW,
            windMaxWidthDesign = textMaxW,
        )
    }

    private fun maxTextWidth(anchor: OrbitAnchor, drawX: Float, midY: Float): Float {
        val right = FaceLayout.chordRightXAtY(midY) - CHORD_INSET
        val left = FaceLayout.chordLeftXAtY(midY) + CHORD_INSET
        return when (anchor.textAlign) {
            Paint.Align.LEFT -> (right - drawX).coerceAtLeast(48f)
            Paint.Align.RIGHT -> (drawX - left).coerceAtLeast(48f)
            else -> (right - left).coerceAtLeast(48f)
        }
    }
}
