package com.watchapp.data

import android.content.Context
import android.util.Log
import com.watchapp.WatchApp
import com.watchapp.watchface.WatchFaceInvalidate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object FlightGpsTracker {
    private const val TAG = "FlightGpsTracker"
    private const val INTERVAL_MS = 5_000L
    private var job: Job? = null

    fun updateRunning(context: Context, shouldRun: Boolean) {
        if (!shouldRun) {
            job?.cancel()
            job = null
            return
        }
        if (job?.isActive == true) return
        val app = context.applicationContext
        job = WatchApp.instance.appScope.launch {
            Log.i(TAG, "GPS polling started")
            while (isActive) {
                runCatching { pollOnce(app) }.onFailure { Log.w(TAG, "GPS poll failed", it) }
                delay(INTERVAL_MS)
            }
            Log.i(TAG, "GPS polling stopped")
        }
    }

    private suspend fun pollOnce(app: Context) {
        val (lat, lon) = LocationResolver.getCurrent(app)
        FlightLocationStore.update(lat, lon)
        WatchFaceInvalidate.request()
    }
}
