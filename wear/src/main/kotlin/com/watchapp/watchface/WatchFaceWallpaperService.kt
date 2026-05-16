package com.watchapp.watchface

import android.content.Intent
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
        val slot = AmbientBurnInLayout.slotFor(zonedDateTime)
        val timeXDesign = AmbientBurnInLayout.timeAnchorX(slot)
        val timeX = bounds.scaleX(timeXDesign)
        val (tz1Line, tz2Line) = AmbientTzLines.resolve(appContext, data, zonedDateTime)
        val tzMidY = slot.timeTop + AmbientBurnInLayout.AMBIENT_TIME_BOX_H +
            AmbientBurnInLayout.AMBIENT_TZ_GAP + AmbientBurnInLayout.AMBIENT_TZ_LINE_H
        val tzMaxW = bounds.scaleX(AmbientBurnInLayout.tzMaxWidth(slot, timeXDesign, tzMidY))

        val ambientTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.ambientTime
            typeface = FaceFonts.clock(appContext)
            fitDesignText(bounds, FaceTypography.SIZE_AMBIENT_CLOCK, 80f, CLOCK_AMBIENT_FIT_SAMPLE)
        }
        val ambientSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.ambientSub
            textAlign = ambientAlign(slot.timeAlignLeft)
            fitDesignText(bounds, FaceTypography.SIZE_AMBIENT_SUB, 24f, "DEL")
        }
        val ambientTempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FaceColors.ambientTemp
            textAlign = ambientAlign(slot.weatherAlignRight)
            fitDesignText(bounds, FaceTypography.SIZE_AMBIENT_TEMP, 32f, data.temperature)
        }

        val time = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        val timeBaseline = bounds.baselineInBoxClipped(
            slot.timeTop,
            AmbientBurnInLayout.AMBIENT_TIME_BOX_H,
            ambientTimePaint,
            timeXDesign,
        )
        canvas.drawTabularText(
            time,
            timeX,
            timeBaseline,
            ambientTimePaint,
            ambientAlign(slot.timeAlignLeft),
        )

        val timeFm = ambientTimePaint.fontMetrics
        val timeBottomDesign =
            (timeBaseline + timeFm.descent - bounds.designOffsetY()) / bounds.layoutScale()
        val tz1Top = timeBottomDesign + AmbientBurnInLayout.AMBIENT_TZ_GAP
        val tz2Top = tz1Top + AmbientBurnInLayout.AMBIENT_TZ_LINE_H + AmbientBurnInLayout.AMBIENT_TZ_GAP
        canvas.drawText(
            ambientSubPaint.fittedSingleLine(tz1Line, tzMaxW),
            timeX,
            bounds.baselineInBoxClipped(
                tz1Top,
                AmbientBurnInLayout.AMBIENT_TZ_LINE_H,
                ambientSubPaint,
                timeXDesign,
            ),
            ambientSubPaint,
        )
        val ambientTz2Paint = Paint(ambientSubPaint).apply {
            typeface = FaceFonts.timezone2(appContext)
        }
        canvas.drawText(
            ambientTz2Paint.fittedSingleLine(tz2Line, tzMaxW),
            timeX,
            bounds.baselineInBoxClipped(
                tz2Top,
                AmbientBurnInLayout.AMBIENT_TZ_LINE_H,
                ambientTz2Paint,
                timeXDesign,
            ),
            ambientTz2Paint,
        )

        val weatherTempXDesign = AmbientBurnInLayout.weatherTextAnchorX(slot, slot.weatherTempTop)
        val weatherWindXDesign = AmbientBurnInLayout.weatherTextAnchorX(slot, slot.weatherWindTop)
        val weatherTempMaxW = bounds.scaleX(
            AmbientBurnInLayout.weatherTextMaxWidth(slot, slot.weatherTempTop),
        )
        val weatherWindMaxW = bounds.scaleX(
            AmbientBurnInLayout.weatherTextMaxWidth(slot, slot.weatherWindTop),
        )
        canvas.drawText(
            ambientTempPaint.fittedSingleLine(data.temperature, weatherTempMaxW),
            bounds.scaleX(weatherTempXDesign),
            bounds.baselineInBoxClipped(
                slot.weatherTempTop,
                AmbientBurnInLayout.WEATHER_TEMP_H,
                ambientTempPaint,
                weatherTempXDesign,
            ),
            ambientTempPaint,
        )
        val windPaint = Paint(ambientSubPaint).apply {
            color = FaceColors.ambientWindColor(data.wind)
            textAlign = ambientAlign(slot.weatherAlignRight)
        }
        canvas.drawText(
            windPaint.fittedSingleLine(data.wind.uppercase(), weatherWindMaxW),
            bounds.scaleX(weatherWindXDesign),
            bounds.baselineInBoxClipped(
                slot.weatherWindTop,
                AmbientBurnInLayout.WEATHER_WIND_H,
                windPaint,
                weatherWindXDesign,
            ),
            windPaint,
        )
        WeatherIconRenderer.drawAmbient(
            canvas,
            bounds,
            appContext,
            AmbientBurnInLayout.weatherIconLeft(slot),
            slot.weatherIconTop,
            AmbientBurnInLayout.WEATHER_ICON_SIZE,
            data.windDirectionDegrees,
        )
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) = Unit

    override fun onDestroy() {
        WatchFaceInvalidate.unregister()
        super.onDestroy()
    }
}

private fun ambientAlign(alignLeft: Boolean): Paint.Align =
    if (alignLeft) Paint.Align.LEFT else Paint.Align.RIGHT

private fun drawFaceBackground(canvas: Canvas, bounds: Rect, paint: Paint) {
    canvas.drawCircle(
        bounds.exactCenterX(),
        bounds.exactCenterY(),
        bounds.designSide() / 2f,
        paint,
    )
}
