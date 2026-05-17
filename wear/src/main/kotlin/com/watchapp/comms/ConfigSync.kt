package com.watchapp.comms

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.watchapp.data.ConfigRepository
import com.watchapp.shared.ConfigJson
import com.watchapp.shared.DataPaths
import com.watchapp.shared.RefreshConfig
import com.watchapp.shared.TimezoneConfig
import com.watchapp.shared.UnitsConfig
import com.watchapp.watchface.WatchFaceConfigCache
import com.watchapp.watchface.WatchFaceInvalidate
import com.watchapp.workers.RefreshWorker
import kotlinx.coroutines.tasks.await

object ConfigSync {
    private const val TAG = "ConfigSync"

    fun normalizePath(raw: String?): String? {
        val path = raw?.trim() ?: return null
        if (path.isEmpty()) return null
        return if (path.startsWith("/")) path else "/$path"
    }

    /** Reload timezone from disk into the in-memory cache used by the watch face renderer. */
    fun reloadWatchFaceTimezone(context: Context) {
        runCatching {
            val tz = ConfigRepository(context).getConfig().timezone
            WatchFaceConfigCache.update(tz)
            Log.i(TAG, "Watch face timezone: secondary=${tz.secondaryTimezone} label=${tz.secondaryLabel}")
        }.onFailure { Log.e(TAG, "Failed to reload watch face timezone", it) }
    }

    suspend fun applyJson(context: Context, path: String, json: String): Boolean {
        val repo = ConfigRepository(context)
        val normalized = normalizePath(path) ?: return false
        return runCatching {
            when (normalized) {
                DataPaths.CONFIG_UNITS -> {
                    val units = ConfigJson.json.decodeFromString(UnitsConfig.serializer(), json)
                    repo.saveUnits(units)
                }
                DataPaths.CONFIG_REFRESH -> {
                    val refresh = ConfigJson.json.decodeFromString(RefreshConfig.serializer(), json)
                    repo.saveRefresh(refresh.weatherIntervalMinutes)
                }
                DataPaths.CONFIG_TIMEZONE -> {
                    val tz = ConfigJson.json.decodeFromString(TimezoneConfig.serializer(), json)
                    repo.saveTimezone(tz)
                    WatchFaceConfigCache.update(tz)
                    Log.i(TAG, "Saved timezone: secondary=${tz.secondaryTimezone}")
                }
                DataPaths.CONFIG_BUTTONS -> {
                    val panels = ConfigJson.decodePanels(json)
                    repo.savePanels(panels)
                }
                else -> return false
            }
            true
        }.onFailure { Log.e(TAG, "Failed to apply $normalized", it) }
            .getOrDefault(false)
    }

    suspend fun bootstrapFromDataLayer(context: Context) {
        val buffer = Wearable.getDataClient(context).dataItems.await()
        var applied = 0
        try {
            for (item in buffer) {
                val path = normalizePath(item.uri.path) ?: continue
                if (!path.startsWith("/config")) continue
                val json = DataMapItem.fromDataItem(item).dataMap.getString("json") ?: continue
                Log.d(TAG, "Bootstrap item: $path")
                if (applyJson(context, path, json)) applied++
            }
        } finally {
            buffer.release()
        }
        reloadWatchFaceTimezone(context)
        if (applied > 0) {
            Log.i(TAG, "Bootstrapped $applied config item(s) from data layer")
            scheduleRefresh(context)
        } else {
            Log.w(TAG, "No config items found on data layer")
        }
    }

    fun scheduleRefresh(context: Context) {
        reloadWatchFaceTimezone(context)
        WatchFaceInvalidate.request()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "config_refresh",
            ExistingWorkPolicy.REPLACE,
            RefreshWorker.oneTimeRequest(),
        )
    }
}
