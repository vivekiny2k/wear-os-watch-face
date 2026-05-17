package com.watchapp.watchface

import android.content.Intent
import android.util.Log
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.watchapp.R
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.watchapp.WatchApp
import com.watchapp.comms.ConfigSync
import com.watchapp.data.FaceDataRefresher
import com.watchapp.data.WatchDataStore
import com.watchapp.ui.ActionPanelActivity
import com.watchapp.workers.RefreshWorker
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class WatchFaceWallpaperService : WatchFaceService() {
    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
    ): WatchFace {
        WatchDataStore.hydrate(applicationContext)
        ConfigSync.bootstrapFromDataLayer(applicationContext)
        ConfigSync.reloadWatchFaceTimezone(applicationContext)
        RefreshWorker.enqueueNow(applicationContext)
        if (applicationContext is WatchApp) {
            (applicationContext as WatchApp).appScope.launch {
                FaceDataRefresher.refresh(applicationContext)
            }
        }
        val renderer = ReferenceFaceRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            applicationContext,
        )
        val tapHandler = CenterTapHandler(surfaceHolder)
        return WatchFace(WatchFaceType.DIGITAL, renderer).also { face ->
            face.setTapListener(
                object : WatchFace.TapListener {
                    override fun onTapEvent(
                        tapType: Int,
                        tapEvent: TapEvent,
                        complicationSlot: androidx.wear.watchface.ComplicationSlot?,
                    ) {
                        if (complicationSlot != null) return
                        if (tapHandler.onTapEvent(tapType, tapEvent)) {
                            val intent = Intent(this@WatchFaceWallpaperService, ActionPanelActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                },
            )
        }
    }
}

private class ReferenceFaceRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    private val watchState: WatchState,
    private val appContext: android.content.Context,
) : Renderer.CanvasRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    16L,
    false,
) {
    init {
        WatchDataStore.hydrate(appContext)
        ConfigSync.reloadWatchFaceTimezone(appContext)
        WatchFaceInvalidate.register {
            ConfigSync.reloadWatchFaceTimezone(appContext)
            invalidate()
        }
    }

    private val bgPaint = Paint().apply { color = FaceColors.background }
    private val ambientBgPaint = Paint().apply { color = FaceColors.ambientBackground }
    private val dividerPaint = Paint().apply { color = FaceColors.divider }
    private var lastAmbientLayoutLogKey = -1

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        val data = WatchDataStore.get()
        val ambient = watchState.isAmbient.value == true
        dividerPaint.strokeWidth = 2f * bounds.layoutScale()

        canvas.save()
        canvas.clipPath(bounds.roundClipPath())

        if (ambient) {
            drawAmbient(canvas, bounds, zonedDateTime, data)
        } else {
            drawActive(canvas, bounds, zonedDateTime, data)
        }

        canvas.restore()
    }

    private fun drawActive(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        data: com.watchapp.data.WatchFaceData,
    ) {
        drawFaceBackground(canvas, bounds, bgPaint)

        // Dividers — fixed spans from watchface.xml (x=24, length=402; verticals at 132/316)
        bounds.drawHorizontalRule(canvas, FaceLayout.DIVIDER_TOP, dividerPaint)
        bounds.drawHorizontalRule(canvas, FaceLayout.DIVIDER_DATE, dividerPaint)
        bounds.drawVerticalRule(
            canvas,
            FaceLayout.BOTTOM_COL_LEFT,
            FaceLayout.BOTTOM_PANEL_TOP,
            FaceLayout.DIVIDER_BOTTOM,
            dividerPaint,
        )
        bounds.drawVerticalRule(
            canvas,
            FaceLayout.BOTTOM_COL_RIGHT,
            FaceLayout.BOTTOM_PANEL_TOP,
            FaceLayout.DIVIDER_BOTTOM,
            dividerPaint,
        )

        val tempMaxW = bounds.designWidth(FaceLayout.tempMaxWidth())
        val windMaxW = bounds.designWidth(FaceLayout.windMaxWidth())
        val condMaxW = bounds.designWidth(FaceLayout.condMaxWidth())
        val tempX = FaceLayout.tempAnchorX()
        val windX = FaceLayout.windAnchorX()
        val condX = FaceLayout.condAnchorX()
        val clockMaxW = bounds.designWidth(FaceLayout.CLOCK_W)
        val dateMaxW = bounds.designWidth(FaceLayout.dateMaxWidth())
        val altMaxW = bounds.designWidth(FaceLayout.ALT_BOX_W)
        val sunriseX = FaceLayout.sunriseAnchorX()
        val sunsetX = FaceLayout.sunsetAnchorX()

        canvas.save()
        bounds.clipDesignBand(canvas, 0f, FaceLayout.DIVIDER_TOP)

        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.activeTemperatureColor(data.temperature)
            typeface = FaceFonts.temperature()
            fitDesignText(bounds, FaceTypography.SIZE_TEMP, FaceLayout.TEMP_H, data.temperature, tempMaxW)
        }
        val windPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.wind
            typeface = FaceFonts.wind()
            textAlign = Paint.Align.RIGHT
            fitDesignText(bounds, FaceTypography.SIZE_WIND, FaceLayout.WIND_H, data.wind, windMaxW)
        }
        val conditionsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.text
            textAlign = Paint.Align.RIGHT
            typeface = FaceFonts.conditions()
            fitDesignText(bounds, FaceTypography.SIZE_COND, FaceLayout.COND_H, data.conditions, condMaxW)
        }

        canvas.drawText(
            data.temperature,
            bounds.scaleX(tempX),
            bounds.baselineInBoxClipped(FaceLayout.TEMP_TOP, FaceLayout.TEMP_H, tempPaint, tempX),
            tempPaint,
        )
        canvas.drawText(
            windPaint.fittedSingleLine(data.wind, windMaxW),
            bounds.scaleX(windX),
            bounds.baselineInBoxClipped(FaceLayout.WIND_TOP, FaceLayout.WIND_H, windPaint, windX),
            windPaint,
        )
        canvas.drawText(
            conditionsPaint.fittedSingleLine(data.conditions, condMaxW),
            bounds.scaleX(condX),
            bounds.baselineInBoxClipped(FaceLayout.COND_TOP, FaceLayout.COND_H, conditionsPaint, condX),
            conditionsPaint,
        )
        WeatherIconRenderer.draw(canvas, bounds, appContext, data.windDirectionDegrees)
        canvas.restore()

        canvas.save()
        bounds.clipDesignBand(canvas, FaceLayout.DIVIDER_TOP, FaceLayout.DIVIDER_DATE)

        val clock = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.text
            typeface = FaceFonts.clock(appContext)
            fitDesignText(
                bounds,
                FaceTypography.SIZE_CLOCK,
                FaceLayout.CLOCK_H,
                CLOCK_FIT_SAMPLE,
                clockMaxW,
            )
        }
        canvas.drawTabularText(
            clock,
            bounds.scaleX(FaceLayout.CLOCK_LEFT + FaceLayout.CLOCK_W / 2f),
            bounds.baselineInBox(FaceLayout.CLOCK_TOP, FaceLayout.CLOCK_H, timePaint),
            timePaint,
            Paint.Align.CENTER,
        )

        val dateText = formatOrdinalDate(zonedDateTime)
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.text
            typeface = FaceFonts.date()
            fitDesignText(bounds, FaceTypography.SIZE_DATE, FaceLayout.DATE_H, dateText, dateMaxW)
        }
        canvas.drawText(
            dateText,
            bounds.scaleX(FaceLayout.DATE_LEFT),
            bounds.baselineInBox(FaceLayout.DATE_TOP, FaceLayout.DATE_H, datePaint),
            datePaint,
        )

        val altTime = zonedDateTime.withZoneSameInstant(WatchFaceConfigCache.secondaryZone())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        val altPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.text
            typeface = FaceFonts.altClock(appContext)
            textAlign = Paint.Align.CENTER
            fitDesignText(bounds, FaceTypography.SIZE_ALT, FaceLayout.ALT_BOX_H, altTime, altMaxW)
        }
        val boxPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = FaceColors.text
            strokeWidth = 1.5f * bounds.layoutScale()
        }
        canvas.drawRect(
            bounds.scaleX(FaceLayout.ALT_BOX_LEFT),
            bounds.scaleY(FaceLayout.ALT_BOX_TOP),
            bounds.scaleX(FaceLayout.ALT_BOX_RIGHT),
            bounds.scaleY(FaceLayout.ALT_BOX_TOP + FaceLayout.ALT_BOX_H),
            boxPaint,
        )
        canvas.drawText(
            altTime,
            bounds.scaleX(FaceLayout.ALT_BOX_LEFT + FaceLayout.ALT_BOX_W / 2f),
            bounds.baselineInBox(FaceLayout.ALT_BOX_TOP, FaceLayout.ALT_BOX_H, altPaint),
            altPaint,
        )
        canvas.restore()

        canvas.save()
        bounds.clipDesignBand(canvas, FaceLayout.DIVIDER_DATE, FaceLayout.DIVIDER_BOTTOM)

        val centerColW = bounds.designWidth(FaceLayout.LOCATION_W)
        val locationCenterX = bounds.scaleX(FaceLayout.LOCATION_LEFT + FaceLayout.LOCATION_W / 2f)
        val sunriseMaxW = bounds.designWidth(
            minOf(FaceLayout.SUNRISE_W, FaceLayout.BOTTOM_COL_LEFT - sunriseX - 8f),
        )
        val sunsetMaxW = bounds.designWidth(
            minOf(FaceLayout.SUNSET_W, sunsetX - FaceLayout.BOTTOM_COL_RIGHT - 8f),
        )

        val sunrisePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.sunrise
            typeface = FaceFonts.solarTime()
            fitDesignText(bounds, FaceTypography.SIZE_SUN, FaceLayout.SUNRISE_H, data.sunrise, sunriseMaxW)
        }
        val sunsetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.sunset
            typeface = FaceFonts.solarTime()
            textAlign = Paint.Align.RIGHT
            fitDesignText(bounds, FaceTypography.SIZE_SUN, FaceLayout.SUNSET_H, data.sunset, sunsetMaxW)
        }
        val locationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.text
            textAlign = Paint.Align.CENTER
            typeface = FaceFonts.location()
            fitDesignText(
                bounds,
                FaceTypography.SIZE_LOCATION,
                FaceLayout.LOCATION_H,
                formatLocation(data.location),
                centerColW,
            )
        }
        val altitudePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.text
            textAlign = Paint.Align.CENTER
            typeface = FaceFonts.altitude()
            fitDesignText(bounds, FaceTypography.SIZE_MSL, FaceLayout.ALTITUDE_H, data.altitude, centerColW)
        }

        canvas.drawText(
            sunrisePaint.fittedSingleLine(data.sunrise, sunriseMaxW),
            bounds.scaleX(sunriseX),
            bounds.baselineInBox(FaceLayout.SUNRISE_TOP, FaceLayout.SUNRISE_H, sunrisePaint),
            sunrisePaint,
        )
        canvas.drawText(
            locationPaint.fittedSingleLine(formatLocation(data.location), centerColW),
            locationCenterX,
            bounds.baselineInBox(FaceLayout.LOCATION_TOP, FaceLayout.LOCATION_H, locationPaint),
            locationPaint,
        )
        canvas.drawText(
            altitudePaint.fittedSingleLine(data.altitude, centerColW),
            locationCenterX,
            bounds.baselineInBox(FaceLayout.ALTITUDE_TOP, FaceLayout.ALTITUDE_H, altitudePaint),
            altitudePaint,
        )
        canvas.drawText(
            sunsetPaint.fittedSingleLine(data.sunset, sunsetMaxW),
            bounds.scaleX(sunsetX),
            bounds.baselineInBox(FaceLayout.SUNSET_TOP, FaceLayout.SUNSET_H, sunsetPaint),
            sunsetPaint,
        )
        canvas.restore()
    }

    private fun drawAmbient(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        data: com.watchapp.data.WatchFaceData,
    ) {
        drawFaceBackground(canvas, bounds, ambientBgPaint)
        val (tz1Line, tz2Line) = AmbientTzLines.resolve(appContext, data, zonedDateTime)

        val ambientTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.ambientTime
            typeface = FaceFonts.clock(appContext)
            fitDesignText(
                bounds,
                AmbientBurnInLayout.AMBIENT_TIME_BOX_H,
                AmbientBurnInLayout.AMBIENT_TIME_BOX_H,
                CLOCK_AMBIENT_FIT_SAMPLE,
            )
        }
        val ambientTzPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.ambientSub
            fitDesignText(bounds, FaceTypography.SIZE_AMBIENT_SUB, FaceTypography.SIZE_AMBIENT_SUB, "DEL 00:00")
        }
        val timeBox = AmbientBurnInLayout.timeBox(
            bounds,
            zonedDateTime,
            ambientTimePaint,
            ambientTzPaint,
            ambientTzPaint,
        )
        ambientTimePaint.textAlign = timeBox.anchor.textAlign
        ambientTzPaint.textAlign = timeBox.anchor.textAlign

        val timeX = bounds.scaleX(timeBox.drawX)
        val tzMaxW = bounds.scaleX(timeBox.maxTextWidthDesign)

        val ambientTempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.ambientTemp
            fitDesignText(bounds, FaceTypography.SIZE_AMBIENT_TEMP, 32f, data.temperature)
        }
        val windPaint = Paint(ambientTzPaint).apply {
            color = FaceColors.ambientWindColor(data.wind)
        }
        val weatherBox = AmbientBurnInLayout.weatherBox(bounds, zonedDateTime, ambientTempPaint, windPaint)
        ambientTempPaint.textAlign = weatherBox.anchor.textAlign
        windPaint.textAlign = weatherBox.anchor.textAlign

        maybeLogAmbientLayout(zonedDateTime, timeBox, weatherBox)

        val time = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        canvas.drawTabularText(
            time,
            timeX,
            timeBox.clockBaseline,
            ambientTimePaint,
            timeBox.anchor.textAlign,
        )
        canvas.drawText(
            ambientTzPaint.fittedSingleLine(tz1Line, tzMaxW),
            timeX,
            timeBox.tz1Baseline,
            ambientTzPaint,
        )
        canvas.drawText(
            ambientTzPaint.fittedSingleLine(tz2Line, tzMaxW),
            timeX,
            timeBox.tz2Baseline,
            ambientTzPaint,
        )

        val weatherX = bounds.scaleX(weatherBox.drawX)
        val tempMaxW = bounds.scaleX(weatherBox.tempMaxWidthDesign)
        val windMaxW = bounds.scaleX(weatherBox.windMaxWidthDesign)
        canvas.drawText(
            ambientTempPaint.fittedSingleLine(data.temperature, tempMaxW),
            weatherX,
            weatherBox.tempBaseline,
            ambientTempPaint,
        )
        canvas.drawText(
            windPaint.fittedSingleLine(data.wind.uppercase(), windMaxW),
            weatherX,
            weatherBox.windBaseline,
            windPaint,
        )
        WeatherIconRenderer.drawAmbient(
            canvas,
            bounds,
            appContext,
            weatherBox.iconLeft,
            weatherBox.iconTop,
            AmbientBurnInLayout.WEATHER_ICON_SIZE,
            data.windDirectionDegrees,
        )
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) = Unit

    private fun maybeLogAmbientLayout(
        zonedDateTime: ZonedDateTime,
        timeBox: AmbientBurnInLayout.TimeBox,
        weatherBox: AmbientBurnInLayout.WeatherBox,
    ) {
        val logKey = zonedDateTime.hour * 60 + zonedDateTime.minute
        if (logKey == lastAmbientLayoutLogKey) return
        lastAmbientLayoutLogKey = logKey
        val hour12 = AmbientBurnInLayout.currentHour12(zonedDateTime)
        val weatherHour = AmbientBurnInLayout.weatherHour12(hour12)
        Log.i(
            TAG,
            "ambient orbit hour12=$hour12 weatherHour=$weatherHour " +
                "timeAnchor=(${timeBox.anchor.centerX},${timeBox.anchor.centerY}) drawX=${timeBox.drawX} " +
                "weatherAnchor=(${weatherBox.anchor.centerX},${weatherBox.anchor.centerY}) drawX=${weatherBox.drawX}",
        )
    }

    override fun onDestroy() {
        WatchFaceInvalidate.unregister()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WatchFaceAmbient"
    }
}

private fun drawFaceBackground(canvas: Canvas, bounds: Rect, paint: Paint) {
    canvas.drawCircle(
        bounds.exactCenterX(),
        bounds.exactCenterY(),
        bounds.designSide() / 2f,
        paint,
    )
}
