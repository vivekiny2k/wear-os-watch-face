package com.watchapp.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object LocationResolver {
    private const val TAG = "LocationResolver"
    private const val DEFAULT_LAT = 39.3601
    private const val DEFAULT_LON = -84.3099

    suspend fun getCurrent(context: Context): Pair<Double, Double> {
        val app = context.applicationContext
        val hasPermission = ContextCompat.checkSelfPermission(
            app,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                app,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w(TAG, "Location permission not granted")
            return DEFAULT_LAT to DEFAULT_LON
        }
        val fused = LocationServices.getFusedLocationProviderClient(app)
        val cancel = CancellationTokenSource()
        var location = fused.lastLocation.await()
        if (location == null) {
            location = fused.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancel.token,
            ).await()
        }
        if (location != null) return location.latitude to location.longitude
        Log.w(TAG, "No GPS fix")
        return DEFAULT_LAT to DEFAULT_LON
    }
}
