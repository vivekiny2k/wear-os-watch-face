package com.watchapp.comms

import android.content.Context
import android.util.Log
import com.watchapp.data.ConfigRepository
import com.watchapp.shared.ConfigJson
import com.watchapp.shared.ConfigPushBundle
import com.watchapp.shared.TimezoneConfig

object ConfigPushHandler {
    private const val TAG = "ConfigPushHandler"

    suspend fun apply(context: Context, payload: ByteArray): Boolean {
        return runCatching {
            val bundle = ConfigJson.json.decodeFromString(
                ConfigPushBundle.serializer(),
                payload.decodeToString(),
            )
            val repo = ConfigRepository(context)
            repo.saveUnits(bundle.units)
            repo.saveRefresh(bundle.refresh.weatherIntervalMinutes)
            repo.saveTimezone(bundle.timezone)
            repo.saveButtons(bundle.buttons)
            ConfigSync.reloadWatchFaceTimezone(context)
            ConfigSync.scheduleRefresh(context)
            Log.i(
                TAG,
                "Applied config push: secondary=${bundle.timezone.secondaryTimezone} " +
                    "ambient2=${bundle.timezone.ambientSecondTimezone}",
            )
            true
        }.onFailure { Log.e(TAG, "Failed to apply config push", it) }
            .getOrDefault(false)
    }

    suspend fun applyTimezone(context: Context, payload: ByteArray): Boolean {
        return runCatching {
            val tz = ConfigJson.json.decodeFromString(
                TimezoneConfig.serializer(),
                payload.decodeToString(),
            )
            ConfigRepository(context).saveTimezone(tz)
            ConfigSync.reloadWatchFaceTimezone(context)
            ConfigSync.scheduleRefresh(context)
            Log.i(TAG, "Applied timezone push: secondary=${tz.secondaryTimezone}")
            true
        }.onFailure { Log.e(TAG, "Failed to apply timezone push", it) }
            .getOrDefault(false)
    }
}
