package com.watchapp.watchface

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Layout for 450×450 reference ([faces/preview.jpg]).
 * Divider Y measured on 512px preview (× 450/512).
 */
object FaceLayout {
    const val DESIGN_SIZE = 450f
    const val DESIGN_CENTER = DESIGN_SIZE / 2f
    const val DESIGN_RADIUS = DESIGN_CENTER

    const val LEFT = 24f
    const val RIGHT = 426f
    const val CENTER = DESIGN_CENTER

    const val CENTER_TAP_CY = 150f
    const val CENTER_TAP_RADIUS = 100f

    /** Upper ~px 152, lower ~px 319 on preview.jpg */
    const val DIVIDER_TOP = 134f
    const val DIVIDER_DATE = 280f
    const val DIVIDER_BOTTOM = 430f

    const val BOTTOM_COL_LEFT = 132f
    const val BOTTOM_COL_RIGHT = 316f
    const val BOTTOM_PANEL_TOP = DIVIDER_DATE
    const val BOTTOM_PANEL_BOTTOM = DIVIDER_BOTTOM

    // Weather — above DIVIDER_TOP; X anchors respect circular chord (WFF x=24/426 clip off-screen)
    const val TEMP_TOP = 48f
    const val TEMP_H = 58f
    const val TEMP_W = 90f

    const val WEATHER_ICON_TOP = 44f
    const val WEATHER_ICON_SIZE = 72f

    /** Wind on top, conditions directly below (right column). */
    const val WIND_TOP = 36f
    const val WIND_H = 38f
    const val WIND_W = 174f

    const val COND_GAP = 6f
    const val COND_TOP = WIND_TOP + WIND_H + COND_GAP
    const val COND_H = 36f
    const val COND_W = 174f

    fun tempAnchorX(): Float = chordLeftXAtY(TEMP_TOP + TEMP_H / 2f) + CHORD_TEXT_INSET

    const val WEATHER_RIGHT_X_OFFSET = 15f

    fun windAnchorX(): Float = weatherRightAnchorX(WIND_TOP + WIND_H / 2f)

    fun condAnchorX(): Float = weatherRightAnchorX(COND_TOP + COND_H / 2f)

    private fun weatherRightAnchorX(designY: Float): Float =
        chordRightXAtY(designY) - CHORD_TEXT_INSET + WEATHER_RIGHT_X_OFFSET

    fun weatherIconLeft(): Float {
        val half = WEATHER_ICON_SIZE / 2f
        return (CENTER - half).coerceIn(
            chordLeftXAtY(WEATHER_ICON_TOP + half) + half,
            chordRightXAtY(WEATHER_ICON_TOP + half) - WEATHER_ICON_SIZE,
        )
    }

    fun tempMaxWidth(): Float = (weatherIconLeft() - tempAnchorX() - 8f).coerceAtLeast(48f)

    fun windMaxWidth(): Float = (windAnchorX() - weatherIconLeft() - WEATHER_ICON_SIZE - 8f).coerceAtLeast(48f)

    fun condMaxWidth(): Float = (condAnchorX() - weatherIconLeft() - WEATHER_ICON_SIZE - 8f).coerceAtLeast(48f)

    const val CLOCK_LEFT = 40f
    const val CLOCK_W = 370f
    const val CLOCK_TOP = 118f
    const val CLOCK_H = 108f

    const val DATE_LEFT = 24f
    const val DATE_TOP = 223f
    const val DATE_H = 48f

    fun dateMaxWidth(): Float = (ALT_BOX_LEFT - DATE_LEFT - 12f).coerceAtLeast(80f)

    const val ALT_BOX_LEFT = 248f
    const val ALT_BOX_TOP = 216f
    const val ALT_BOX_RIGHT = 426f
    const val ALT_BOX_W = ALT_BOX_RIGHT - ALT_BOX_LEFT
    const val ALT_BOX_H = 64f

    const val SUNRISE_LEFT = 20f
    const val SUNRISE_TOP = 292f
    const val SUNRISE_W = 80f
    const val SUNRISE_H = 40f

    const val LOCATION_LEFT = 148f
    const val LOCATION_TOP = 292f
    const val LOCATION_W = 154f
    const val LOCATION_H = 52f

    const val ALTITUDE_LEFT = 148f
    const val ALTITUDE_TOP = 338f
    const val ALTITUDE_W = 154f
    const val ALTITUDE_H = 50f

    const val SUNSET_LEFT = 350f
    const val SUNSET_TOP = 292f
    const val SUNSET_W = 80f
    const val SUNSET_H = 40f
    const val SUNSET_RIGHT = SUNSET_LEFT + SUNSET_W

    const val CHORD_INSET = 4f
    const val CHORD_TEXT_INSET = 14f
    const val CLIP_MARGIN_Y = 8f

    fun sunriseAnchorX(): Float =
        maxOf(SUNRISE_LEFT, chordLeftXAtY(SUNRISE_TOP) + CHORD_TEXT_INSET)

    fun sunsetAnchorX(): Float =
        minOf(SUNSET_RIGHT, chordRightXAtY(SUNSET_TOP) - CHORD_TEXT_INSET)

    fun chordHalfWidthAtY(designY: Float): Float {
        val dy = designY - DESIGN_CENTER
        if (kotlin.math.abs(dy) >= DESIGN_RADIUS) return 0f
        return sqrt(DESIGN_RADIUS * DESIGN_RADIUS - dy * dy)
    }

    fun chordLeftXAtY(designY: Float): Float = DESIGN_CENTER - chordHalfWidthAtY(designY) + CHORD_INSET

    fun chordRightXAtY(designY: Float): Float = DESIGN_CENTER + chordHalfWidthAtY(designY) - CHORD_INSET

    fun minYInsideCircle(designX: Float): Float {
        val dx = designX - DESIGN_CENTER
        if (kotlin.math.abs(dx) >= DESIGN_RADIUS) return DESIGN_CENTER
        return DESIGN_CENTER - sqrt(DESIGN_RADIUS * DESIGN_RADIUS - dx * dx)
    }
}

fun Rect.designSide(): Float = min(width(), height()).toFloat()

fun Rect.designOffsetX(): Float = (width() - designSide()) / 2f

fun Rect.designOffsetY(): Float = (height() - designSide()) / 2f

fun Rect.layoutScale(): Float = designSide() / FaceLayout.DESIGN_SIZE

fun Rect.scaleX(x: Float): Float =
    designOffsetX() + x / FaceLayout.DESIGN_SIZE * designSide()

fun Rect.scaleY(y: Float): Float =
    designOffsetY() + y / FaceLayout.DESIGN_SIZE * designSide()

fun Rect.designWidth(w: Float): Float = w * layoutScale()

fun Rect.roundClipPath(): Path {
    val cx = exactCenterX()
    val cy = exactCenterY()
    val r = designSide() / 2f * 0.995f
    return Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
}

fun Rect.baselineInBox(boxTop: Float, boxHeight: Float, paint: Paint): Float {
    val fm = paint.fontMetrics
    val textH = fm.descent - fm.ascent
    val boxTopPx = scaleY(boxTop)
    val boxHPx = boxHeight * layoutScale()
    return boxTopPx + (boxHPx - textH) / 2f - fm.ascent
}

fun Rect.baselineInBoxClipped(
    boxTop: Float,
    boxHeight: Float,
    paint: Paint,
    designAnchorX: Float,
): Float {
    var baseline = baselineInBox(boxTop, boxHeight, paint)
    val fm = paint.fontMetrics
    val glyphTopDesign = (baseline + fm.ascent - designOffsetY()) / layoutScale()
    val minY = FaceLayout.minYInsideCircle(designAnchorX) + FaceLayout.CLIP_MARGIN_Y
    if (glyphTopDesign < minY) {
        baseline += scaleY(minY) - (baseline + fm.ascent)
    }
    return baseline
}

fun Rect.drawHorizontalRule(canvas: Canvas, designY: Float, paint: Paint) {
    val y = scaleY(designY)
    canvas.drawLine(scaleX(FaceLayout.LEFT), y, scaleX(FaceLayout.RIGHT), y, paint)
}

fun Rect.drawVerticalRule(canvas: Canvas, designX: Float, y0: Float, y1: Float, paint: Paint) {
    val x = scaleX(designX)
    canvas.drawLine(x, scaleY(y0), x, scaleY(y1), paint)
}

fun Rect.clipDesignBand(canvas: Canvas, top: Float, bottom: Float) {
    canvas.clipRect(
        scaleX(0f),
        scaleY(top),
        scaleX(FaceLayout.DESIGN_SIZE),
        scaleY(bottom),
    )
}

fun Rect.tapToDesign(xPos: Int, yPos: Int): Pair<Float, Float> {
    val side = designSide()
    val designX = (xPos - designOffsetX()) / side * FaceLayout.DESIGN_SIZE
    val designY = (yPos - designOffsetY()) / side * FaceLayout.DESIGN_SIZE
    return designX to designY
}

fun isCenterTap(designX: Float, designY: Float): Boolean {
    val dx = designX - FaceLayout.CENTER
    val dy = designY - FaceLayout.CENTER_TAP_CY
    val r = FaceLayout.CENTER_TAP_RADIUS
    return dx * dx + dy * dy <= r * r
}

fun formatLocation(raw: String): String =
    raw.trim().uppercase(Locale.US).take(6)

fun formatOrdinalDate(zonedDateTime: ZonedDateTime): String {
    val day = zonedDateTime.dayOfMonth
    val suffix = when {
        day in 11..13 -> "TH"
        day % 10 == 1 -> "ST"
        day % 10 == 2 -> "ND"
        day % 10 == 3 -> "RD"
        else -> "TH"
    }
    val month = zonedDateTime.format(DateTimeFormatter.ofPattern("MMM", Locale.US))
    return "${day}$suffix $month".uppercase(Locale.US)
}
