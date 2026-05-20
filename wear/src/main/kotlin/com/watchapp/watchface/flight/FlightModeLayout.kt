package com.watchapp.watchface.flight

import com.watchapp.watchface.FaceLayout
import kotlin.math.min

object FlightModeLayout {
    const val LEFT = FaceLayout.LEFT
    const val RIGHT = FaceLayout.RIGHT
    const val CENTER = FaceLayout.DESIGN_CENTER

    const val DIVIDER_TOP = 124f
    const val DIVIDER_MID = 274f
    const val DIVIDER_BOTTOM = FaceLayout.DIVIDER_BOTTOM

    const val MID_COL_X = 300f
    const val BOT_COL_X = 150f

    const val TIME_TOP = 36f
    const val TIME_H = 80f

    const val BASE_LABEL_TOP = 130f
    const val BASE_LABEL_H = 24f
    const val HEIGHT_TOP = 152f
    const val HEIGHT_H = 44f
    const val DIST_TOP = 196f
    const val DIST_H = 72f

    const val WIND_BOX_CENTER_X = 360f
    const val WIND_BOX_CENTER_Y = 200f
    const val WIND_BOX_W = 86f
    const val WIND_BOX_H = 108f
    const val WIND_BOX_TILT_DEG = 7f

    const val TEMP_TOP = 288f
    const val TEMP_H = 54f
    const val TEMP_LEFT = 40f

    const val TIMER_TOP = 288f
    const val TIMER_H = 54f

    fun chordLeft(y: Float): Float = FaceLayout.chordLeftXAtY(y) + FaceLayout.CHORD_TEXT_INSET

    fun chordRight(y: Float): Float = FaceLayout.chordRightXAtY(y) - FaceLayout.CHORD_TEXT_INSET

    fun cellCenterX(leftBound: Float, rightBound: Float, y: Float): Float {
        val left = maxOf(leftBound, chordLeft(y))
        val right = min(rightBound, chordRight(y))
        return (left + right) / 2f
    }

    fun midLeftCenterX(y: Float): Float = cellCenterX(LEFT, MID_COL_X, y)

    fun bottomLeftCenterX(y: Float): Float = cellCenterX(LEFT, BOT_COL_X, y)

    fun bottomRightCenterX(y: Float): Float = cellCenterX(BOT_COL_X, RIGHT, y)
}