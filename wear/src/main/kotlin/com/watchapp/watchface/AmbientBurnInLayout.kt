package com.watchapp.watchface

import java.time.ZonedDateTime

/**
 * Ambient burn-in: time in one corner, weather diagonally opposite.
 * Position index advances every 15 minutes while in ambient.
 */
object AmbientBurnInLayout {
    const val SLOT_COUNT = 4

    private const val CHORD_INSET = 28f
    private const val CORNER_INSET = 40f

    const val WEATHER_TEMP_H = 32f
    const val WEATHER_WIND_H = 24f
    const val WEATHER_WIND_GAP = 6f
    const val WEATHER_ICON_SIZE = 52f
    const val WEATHER_BLOCK_H = WEATHER_TEMP_H + WEATHER_WIND_GAP + WEATHER_WIND_H

    const val AMBIENT_TIME_BOX_H = 80f
    const val AMBIENT_TZ_LINE_H = 24f
    const val AMBIENT_TZ_GAP = 8f

    data class Slot(
        val timeX: Float,
        val timeTop: Float,
        val timeAlignLeft: Boolean,
        val weatherAlignRight: Boolean,
        val weatherTextLeft: Float,
        val weatherTempTop: Float,
        val weatherWindTop: Float,
        val weatherIconTop: Float,
    )

    private data class TimeCorner(
        val x: Float,
        val top: Float,
        val alignLeft: Boolean,
    )

    private val timeCorners = listOf(
        TimeCorner(CORNER_INSET, CORNER_INSET, alignLeft = true),
        TimeCorner(FaceLayout.RIGHT - CORNER_INSET, CORNER_INSET, alignLeft = false),
        TimeCorner(FaceLayout.RIGHT - CORNER_INSET, FaceLayout.DESIGN_SIZE - CORNER_INSET - AMBIENT_TIME_BOX_H, alignLeft = false),
        TimeCorner(CORNER_INSET, FaceLayout.DESIGN_SIZE - CORNER_INSET - AMBIENT_TIME_BOX_H, alignLeft = true),
    )

    fun slotFor(zonedDateTime: ZonedDateTime): Slot {
        val index = (zonedDateTime.hour * 4 + zonedDateTime.minute / 15) % SLOT_COUNT
        return slotForTimeCorner(timeCorners[index])
    }

    private fun slotForTimeCorner(time: TimeCorner): Slot {
        val weatherCornerX = FaceLayout.DESIGN_SIZE - time.x
        val weatherCornerY = FaceLayout.DESIGN_SIZE - time.top
        val weatherAlignRight = time.alignLeft

        val weatherTempTop = if (weatherCornerY < FaceLayout.DESIGN_CENTER) {
            weatherCornerY + 8f
        } else {
            weatherCornerY - WEATHER_BLOCK_H - 8f
        }.coerceIn(CORNER_INSET, FaceLayout.DESIGN_SIZE - WEATHER_BLOCK_H - CORNER_INSET)
        val weatherWindTop = weatherTempTop + WEATHER_TEMP_H + WEATHER_WIND_GAP
        val weatherIconTop = (weatherTempTop - 4f).coerceAtLeast(CORNER_INSET)

        val weatherTextLeft = if (weatherAlignRight) {
            (weatherCornerX - 130f).coerceAtLeast(FaceLayout.LEFT + 8f)
        } else {
            FaceLayout.LEFT + 8f
        }

        return Slot(
            timeX = time.x,
            timeTop = time.top,
            timeAlignLeft = time.alignLeft,
            weatherAlignRight = weatherAlignRight,
            weatherTextLeft = weatherTextLeft,
            weatherTempTop = weatherTempTop,
            weatherWindTop = weatherWindTop,
            weatherIconTop = weatherIconTop,
        )
    }

    fun timeAnchorX(slot: Slot): Float {
        val y = slot.timeTop + AMBIENT_TIME_BOX_H / 2f
        return if (slot.timeAlignLeft) {
            slot.timeX.coerceAtLeast(FaceLayout.chordLeftXAtY(y) + CHORD_INSET)
        } else {
            slot.timeX.coerceAtMost(FaceLayout.chordRightXAtY(y) - CHORD_INSET)
        }
    }

    fun tzMaxWidth(slot: Slot, timeX: Float, tzMidY: Float): Float {
        val right = FaceLayout.chordRightXAtY(tzMidY) - CHORD_INSET
        val left = FaceLayout.chordLeftXAtY(tzMidY) + CHORD_INSET
        return if (slot.timeAlignLeft) {
            (right - timeX).coerceAtLeast(40f)
        } else {
            (timeX - left).coerceAtLeast(40f)
        }
    }

    fun weatherTextAnchorX(slot: Slot, designY: Float): Float {
        return if (slot.weatherAlignRight) {
            FaceLayout.chordRightXAtY(designY) - CHORD_INSET
        } else {
            FaceLayout.chordLeftXAtY(designY) + CHORD_INSET
        }
    }

    fun weatherTextMaxWidth(slot: Slot, designY: Float): Float {
        val anchor = weatherTextAnchorX(slot, designY)
        return if (slot.weatherAlignRight) {
            (anchor - slot.weatherTextLeft).coerceAtLeast(40f)
        } else {
            (FaceLayout.chordRightXAtY(designY) - CHORD_INSET - anchor).coerceAtLeast(40f)
        }
    }

    fun weatherIconLeft(slot: Slot): Float {
        val centerY = slot.weatherIconTop + WEATHER_ICON_SIZE / 2f
        val anchor = weatherTextAnchorX(slot, centerY)
        return if (slot.weatherAlignRight) {
            (anchor - WEATHER_ICON_SIZE - 8f).coerceAtLeast(slot.weatherTextLeft)
        } else {
            (anchor + 8f).coerceAtMost(
                FaceLayout.chordRightXAtY(centerY) - CHORD_INSET - WEATHER_ICON_SIZE,
            )
        }
    }
}
