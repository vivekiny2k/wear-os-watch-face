package com.watchapp.watchface

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

/** Widest clock sample for fitting (monospace / tabular layout). */
const val CLOCK_FIT_SAMPLE = "88:88:88"
const val CLOCK_AMBIENT_FIT_SAMPLE = "88:88"

/** Font sizes in design px (450 canvas), matched to [faces/preview.jpg]. */
object FaceTypography {
    const val SIZE_TEMP = 56f
    const val SIZE_WIND = 44f
    const val SIZE_COND = 40f
    const val SIZE_CLOCK = 104f
    const val SIZE_DATE = 42f
    const val SIZE_ALT = 55f
    const val SIZE_SUN = 36.4f
    const val SIZE_LOCATION = 48f
    const val SIZE_MSL = 40f

    const val SIZE_AMBIENT_CLOCK = 104f
    const val SIZE_AMBIENT_SUB = 36f
    const val SIZE_AMBIENT_TEMP = 48f
}

fun Paint.fitDesignText(
    bounds: Rect,
    sizeDesign: Float,
    maxHeightDesign: Float,
    text: String,
    maxWidthPx: Float = Float.MAX_VALUE,
) {
    textSize = sizeDesign * bounds.layoutScale()
    // Slot heights in FaceLayout predate doubled typography; never shrink below design size.
    val fm = fontMetrics
    val ink = fm.descent - fm.ascent
    val maxInk = maxOf(maxHeightDesign, sizeDesign) * bounds.layoutScale()
    if (ink > maxInk && ink > 0f) {
        textSize *= maxInk / ink
    }
    if (maxWidthPx < Float.MAX_VALUE) {
        shrinkToWidth(text, maxWidthPx, minSizePx = sizeDesign * bounds.layoutScale() * 0.72f)
    }
}

private fun Paint.shrinkToWidth(text: String, maxWidthPx: Float, minSizePx: Float = 6f) {
    var size = textSize
    while (size > minSizePx && measureText(text) > maxWidthPx) {
        size *= 0.96f
        textSize = size
    }
}

/** Fit text to width: shrink font, then drop trailing chars — no ellipsis. */
/**
 * Draw a time string in fixed-width cells so seconds/minutes ticking does not shift layout.
 * Each digit uses the width of "8"; colons use ":" width (same in monospace fonts).
 */
fun Canvas.drawTabularText(
    text: String,
    anchorX: Float,
    baselineY: Float,
    paint: Paint,
    align: Paint.Align = Paint.Align.CENTER,
) {
    val savedAlign = paint.textAlign
    val digitCell = paint.measureText("8")
    val colonCell = paint.measureText(":")
    var total = 0f
    for (c in text) {
        total += if (c == ':') colonCell else digitCell
    }
    var x = when (align) {
        Paint.Align.CENTER -> anchorX - total / 2f
        Paint.Align.RIGHT -> anchorX - total
        else -> anchorX
    }
    paint.textAlign = Paint.Align.CENTER
    for (c in text) {
        val cell = if (c == ':') colonCell else digitCell
        drawText(c.toString(), x + cell / 2f, baselineY, paint)
        x += cell
    }
    paint.textAlign = savedAlign
}

fun Paint.fittedSingleLine(text: String, maxWidth: Float): String {
    if (measureText(text) <= maxWidth) return text
    val floorSize = textSize * 0.72f
    shrinkToWidth(text, maxWidth, minSizePx = floorSize)
    if (measureText(text) <= maxWidth) return text
    var trimmed = text
    while (trimmed.length > 1 && measureText(trimmed) > maxWidth) {
        trimmed = trimmed.dropLast(1)
    }
    return trimmed
}
