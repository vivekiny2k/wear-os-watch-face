package com.watchapp.watchface.flight

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.watchapp.R
import com.watchapp.data.FlightModeStore
import com.watchapp.data.WatchFaceData
import com.watchapp.watchface.CLOCK_AMBIENT_FIT_SAMPLE
import com.watchapp.watchface.FaceColors
import com.watchapp.watchface.FaceFonts
import com.watchapp.watchface.FaceLayout
import com.watchapp.watchface.baselineAtDesignCenter
import com.watchapp.watchface.baselineInBox
import com.watchapp.watchface.baselineInBoxClipped
import com.watchapp.watchface.designWidth
import com.watchapp.watchface.drawChordHorizontalRule
import com.watchapp.watchface.drawChordVerticalRule
import com.watchapp.watchface.drawTabularText
import com.watchapp.watchface.fitDesignText
import com.watchapp.watchface.fittedSingleLine
import com.watchapp.watchface.layoutScale
import com.watchapp.watchface.scaleX
import com.watchapp.watchface.scaleY
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min

object FlightModeDrawer {
    private const val SIZE_TIME = 92f
    private const val SIZE_BASE = 20f
    private const val SIZE_HEIGHT = 48f
    private const val SIZE_DIST = 72f
    private const val SIZE_WIND = 30f
    private const val SIZE_TEMP = 50f
    private const val SIZE_TIMER = 38f
    private const val SIZE_AMBIENT_CLOCK = 256f

    fun drawActive(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        data: WatchFaceData,
        context: Context,
    ) {
        drawGrid(canvas, bounds)

        val time = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        val timeCenterY = (FlightModeLayout.TIME_TOP + FlightModeLayout.DIVIDER_TOP) / 2f
        val timeMaxW = bounds.designWidth(
            FlightModeLayout.chordRight(timeCenterY) - FlightModeLayout.chordLeft(timeCenterY),
        )
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlightModeColors.time
            typeface = FaceFonts.wind()
            textAlign = Paint.Align.CENTER
            fitDesignText(bounds, SIZE_TIME, FlightModeLayout.TIME_H, "88:88", timeMaxW)
        }
        canvas.drawText(
            timePaint.fittedSingleLine(time, timeMaxW),
            bounds.scaleX(FlightModeLayout.CENTER),
            bounds.baselineInBoxClipped(
                timeCenterY - FlightModeLayout.TIME_H / 2f,
                FlightModeLayout.TIME_H,
                timePaint,
                FlightModeLayout.CENTER,
            ),
            timePaint,
        )

        val baseLabel = "BASE: ${FlightModeStore.get().baseAltitudeFeet}"
        val baseY = FlightModeLayout.BASE_LABEL_TOP
        val baseMaxW = bounds.designWidth(FlightModeLayout.MID_COL_X - FlightModeLayout.chordLeft(baseY) - 8f)
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlightModeColors.label
            typeface = FaceFonts.temperature()
            fitDesignText(bounds, SIZE_BASE, FlightModeLayout.BASE_LABEL_H, baseLabel, baseMaxW)
        }
        canvas.drawText(
            basePaint.fittedSingleLine(baseLabel, baseMaxW),
            bounds.scaleX(FlightModeLayout.chordLeft(baseY)),
            bounds.baselineInBox(FlightModeLayout.BASE_LABEL_TOP, FlightModeLayout.BASE_LABEL_H, basePaint),
            basePaint,
        )

        val heightText = FlightModeStore.flightHeightFeet().toString()
        val heightY = FlightModeLayout.HEIGHT_TOP
        val heightMaxW = baseMaxW
        val heightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlightModeColors.flightHeight
            typeface = FaceFonts.temperature()
            fitDesignText(bounds, SIZE_HEIGHT, FlightModeLayout.HEIGHT_H, "9999", heightMaxW)
        }
        canvas.drawText(
            heightPaint.fittedSingleLine(heightText, heightMaxW),
            bounds.scaleX(FlightModeLayout.chordLeft(heightY)),
            bounds.baselineInBox(FlightModeLayout.HEIGHT_TOP, FlightModeLayout.HEIGHT_H, heightPaint),
            heightPaint,
        )

        val distText = FlightModeStore.formattedDistance()
        val distY = FlightModeLayout.DIST_TOP
        val distMaxW = bounds.designWidth(FlightModeLayout.MID_COL_X - FlightModeLayout.chordLeft(distY) - 12f)
        val distPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlightModeColors.distance
            typeface = FaceFonts.temperature()
            fitDesignText(bounds, SIZE_DIST, FlightModeLayout.DIST_H, "99999", distMaxW)
        }
        canvas.drawText(
            distPaint.fittedSingleLine(distText, distMaxW),
            bounds.scaleX(FlightModeLayout.chordLeft(distY)),
            bounds.baselineInBox(FlightModeLayout.DIST_TOP, FlightModeLayout.DIST_H, distPaint),
            distPaint,
        )

        drawWindCell(canvas, bounds, data, context)

        val tempY = FlightModeLayout.TEMP_TOP
        val tempMaxW = bounds.designWidth(FlightModeLayout.BOT_COL_X - FlightModeLayout.chordLeft(tempY) - 24f)
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlightModeColors.temp
            typeface = FaceFonts.temperature()
            textAlign = Paint.Align.CENTER
            fitDesignText(bounds, SIZE_TEMP, FlightModeLayout.TEMP_H, data.temperature, tempMaxW)
        }
        val tempCenterX = FlightModeLayout.bottomLeftCenterX(tempY)
        canvas.drawText(
            tempPaint.fittedSingleLine(data.temperature, tempMaxW),
            bounds.scaleX(tempCenterX),
            bounds.baselineInBoxClipped(FlightModeLayout.TEMP_TOP, FlightModeLayout.TEMP_H, tempPaint, tempCenterX),
            tempPaint,
        )
        drawThermometer(canvas, bounds, tempCenterX, data.temperature)

        val timerText = FlightModeStore.formattedFlightTime()
        val timerY = FlightModeLayout.TIMER_TOP
        val timerMaxW = bounds.designWidth(FlightModeLayout.chordRight(timerY) - FlightModeLayout.BOT_COL_X - 8f)
        val timerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlightModeColors.timer
            typeface = FaceFonts.altClock(context)
            textAlign = Paint.Align.CENTER
            fitDesignText(bounds, SIZE_TIMER, FlightModeLayout.TIMER_H, "00:00:00", timerMaxW)
        }
        canvas.drawText(
            timerPaint.fittedSingleLine(timerText, timerMaxW),
            bounds.scaleX(FlightModeLayout.bottomRightCenterX(timerY)),
            bounds.baselineInBox(FlightModeLayout.TIMER_TOP, FlightModeLayout.TIMER_H, timerPaint),
            timerPaint,
        )
    }

    fun drawAmbient(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        context: Context,
    ) {
        val time = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        val centerY = FaceLayout.DESIGN_CENTER
        val maxW = bounds.designWidth(
            FlightModeLayout.chordRight(centerY) - FlightModeLayout.chordLeft(centerY),
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.ambientTime
            typeface = FaceFonts.clock(context)
            textAlign = Paint.Align.CENTER
            fitDesignText(bounds, SIZE_AMBIENT_CLOCK, SIZE_AMBIENT_CLOCK, CLOCK_AMBIENT_FIT_SAMPLE, maxW)
        }
        canvas.drawTabularText(
            time,
            bounds.exactCenterX(),
            bounds.baselineAtDesignCenter(centerY, paint),
            paint,
            Paint.Align.CENTER,
        )
    }

    private fun drawGrid(canvas: Canvas, bounds: Rect) {
        val paint = Paint().apply {
            color = FlightModeColors.divider
            strokeWidth = 2f * bounds.layoutScale()
        }
        bounds.drawChordHorizontalRule(canvas, FlightModeLayout.DIVIDER_TOP, paint)
        bounds.drawChordHorizontalRule(canvas, FlightModeLayout.DIVIDER_MID, paint)
        bounds.drawChordVerticalRule(
            canvas,
            FlightModeLayout.MID_COL_X,
            FlightModeLayout.DIVIDER_TOP,
            FlightModeLayout.DIVIDER_MID,
            paint,
        )
        bounds.drawChordVerticalRule(
            canvas,
            FlightModeLayout.BOT_COL_X,
            FlightModeLayout.DIVIDER_MID,
            FlightModeLayout.DIVIDER_BOTTOM,
            paint,
        )
    }

    private fun drawWindCell(
        canvas: Canvas,
        bounds: Rect,
        data: WatchFaceData,
        context: Context,
    ) {
        val scale = bounds.layoutScale()
        val cx = bounds.scaleX(FlightModeLayout.WIND_BOX_CENTER_X)
        val cy = bounds.scaleY(FlightModeLayout.WIND_BOX_CENTER_Y)
        val w = FlightModeLayout.WIND_BOX_W * scale
        val h = FlightModeLayout.WIND_BOX_H * scale
        val corner = 3f * scale

        val windMaxW = w * 0.92f
        val windPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlightModeColors.wind
            typeface = FaceFonts.wind()
            textAlign = Paint.Align.CENTER
            fitDesignText(bounds, SIZE_WIND, 26f, data.wind, windMaxW)
            setShadowLayer(2f * scale, 0f, 0f, FlightModeColors.background)
        }

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(FlightModeLayout.WIND_BOX_TILT_DEG)
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlightModeColors.windBox
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * scale
        }
        canvas.drawRoundRect(RectF(-w / 2f, -h / 2f, w / 2f, h / 2f), corner, corner, boxPaint)

        val arrow = ContextCompat.getDrawable(context, R.drawable.ic_wind_arrow)
        if (arrow != null) {
            DrawableCompat.setTint(arrow, FlightModeColors.windArrow)
            val deg = data.windDirectionDegrees
            canvas.save()
            canvas.translate(0f, -h * 0.1f)
            if (deg != null) canvas.rotate(deg.toFloat() + 90f)
            val arrowSize = (min(w, h) * 0.4f).toInt()
            arrow.setBounds(-arrowSize / 2, -arrowSize / 2, arrowSize / 2, arrowSize / 2)
            arrow.draw(canvas)
            canvas.restore()
        }

        val fm = windPaint.fontMetrics
        val windBaseline = h / 2f - 8f * scale - fm.descent
        canvas.drawText(
            windPaint.fittedSingleLine(data.wind, windMaxW),
            0f,
            windBaseline,
            windPaint,
        )
        canvas.restore()
    }

    private fun drawThermometer(canvas: Canvas, bounds: Rect, centerXDesign: Float, temperature: String) {
        val scale = bounds.layoutScale()
        val cx = bounds.scaleX(centerXDesign)
        val top = bounds.scaleY(FlightModeLayout.TEMP_TOP + 30f)
        val tubeW = 7f * scale
        val tubeH = 28f * scale
        val left = cx - tubeW / 2f
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlightModeColors.label
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlightModeColors.distance
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(left, top, left + tubeW, top + tubeH), tubeW / 2f, tubeW / 2f, outline)
        val level = parseTempLevel(temperature)
        val fillH = tubeH * level
        canvas.drawRoundRect(
            RectF(left + 1.2f * scale, top + tubeH - fillH, left + tubeW - 1.2f * scale, top + tubeH - 1.5f * scale),
            tubeW / 3f,
            tubeW / 3f,
            fill,
        )
    }

    private fun parseTempLevel(temperature: String): Float {
        val match = Regex("(-?[0-9.]+)").find(temperature.replace("°", "")) ?: return 0.5f
        val f = match.groupValues[1].toFloatOrNull() ?: return 0.5f
        return ((f - 32f) / (95f - 32f)).coerceIn(0.15f, 0.95f)
    }
}