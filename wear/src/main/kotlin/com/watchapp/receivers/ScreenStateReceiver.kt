package com.watchapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.watchapp.WatchApp
import com.watchapp.data.DataRefreshPolicy
import com.watchapp.data.FlightGpsTracker
import com.watchapp.sensor.BarometricAltitude
import com.watchapp.workers.RefreshWorker
import kotlinx.coroutines.launch

class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        when (intent?.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen on")
                BarometricAltitude.register(app)
                WatchApp.instance.appScope.launch {
                    DataRefreshPolicy.refreshIfDue(app, "wake")
                }
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "Screen off")
                BarometricAltitude.unregister(app)
                FlightGpsTracker.updateRunning(app, false)
                RefreshWorker.cancelPending(app)
            }
        }
    }

    companion object {
        private const val TAG = "ScreenStateReceiver"
    }
}