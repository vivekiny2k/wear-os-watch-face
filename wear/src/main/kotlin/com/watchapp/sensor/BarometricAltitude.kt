package com.watchapp.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.watchapp.data.WatchDataStore
import kotlin.math.floor
import kotlin.math.pow

/**
 * Barometric altitude (feet), matching WatchMaker:
 * floor((1 - (sprs / P0)^0.190284) * 145366.45 + 0.5) - base_alt
 *
 * [seaLevelPressureHpa] uses Open-Meteo QNH when available; otherwise P0 = 1013.25 hPa.
 */
object BarometricAltitude : SensorEventListener {
    private var pressureHpa: Float? = null
    private var registered = false

    @Volatile
    var seaLevelPressureHpa: Float? = null

    @Volatile
    var baseAltitudeFeet: Int = 0

    fun register(context: Context) {
        if (registered) return
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE) ?: return
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        registered = true
    }

    fun unregister(context: Context) {
        if (!registered) return
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.unregisterListener(this)
        registered = false
    }

    fun formatMsl(): String? {
        val hpa = pressureHpa ?: return null
        return formatMslFromHpa(hpa)
    }

    fun formatMslFromHpa(hpa: Float): String {
        val feet = altitudeFeet(hpa)
        return "$feet MSL"
    }

    fun formatElevationMeters(meters: Double): String {
        val feet = (meters * METERS_TO_FEET).toInt().coerceAtLeast(0)
        return "$feet MSL"
    }

    /** Same as WatchMaker: floor((1-(sprs/P0)^exp)*scale + 0.5) - base_alt */
    fun altitudeFeet(sensorHpa: Float): Int {
        val p0 = seaLevelPressureHpa?.coerceIn(950f, 1050f) ?: SEA_LEVEL_HPA
        val raw = floor((1.0 - (sensorHpa / p0).toDouble().pow(PRESSURE_EXPONENT)) * FEET_SCALE + 0.5).toInt()
        return (raw - baseAltitudeFeet).coerceAtLeast(0)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PRESSURE) return
        pressureHpa = event.values[0]
        val label = formatMsl() ?: return
        val current = WatchDataStore.get()
        if (current.altitude == label) return
        WatchDataStore.updateAltitude(label)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private const val SEA_LEVEL_HPA = 1013.25f
    private const val PRESSURE_EXPONENT = 0.190284
    private const val FEET_SCALE = 145366.45
    private const val METERS_TO_FEET = 3.28084
}
