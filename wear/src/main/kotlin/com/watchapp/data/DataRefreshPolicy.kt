package com.watchapp.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.refreshMetaStore by preferencesDataStore("refresh_meta")

object DataRefreshPolicy {
    private const val TAG = "DataRefreshPolicy"
    private val lastRefreshKey = longPreferencesKey("last_refresh_epoch_ms")

    suspend fun refreshIfDue(context: Context, reason: String, force: Boolean = false): Boolean {
        val app = context.applicationContext
        val intervalMs = intervalMinutes(app).toLong() * 60_000L
        val now = System.currentTimeMillis()
        val last = app.refreshMetaStore.data.first()[lastRefreshKey] ?: 0L
        if (!force && now - last < intervalMs) {
            Log.d(TAG, "Skip refresh ($reason)")
            return false
        }
        val ok = FaceDataRefresher.refresh(app)
        if (ok) app.refreshMetaStore.edit { it[lastRefreshKey] = now }
        return ok
    }

    fun intervalMinutes(context: Context): Int =
        ConfigRepository(context.applicationContext).getConfig().refresh.weatherIntervalMinutes.coerceAtLeast(1)
}
