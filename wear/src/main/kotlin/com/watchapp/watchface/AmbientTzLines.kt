package com.watchapp.watchface

import android.content.Context
import com.watchapp.data.ConfigRepository
import com.watchapp.data.WatchFaceData
import com.watchapp.shared.TimezoneConfig
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object AmbientTzLines {
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun resolve(context: Context, data: WatchFaceData, now: ZonedDateTime): Pair<String, String> {
        val tz = ConfigRepository(context).getConfig().timezone
        val tz1 = if (data.ambientTz1.isValid()) {
            data.ambientTz1
        } else {
            formatLine(tz, tz.secondaryTimezone, tz.secondaryLabel, now)
        }
        val tz2 = if (data.ambientTz2.isValid()) {
            data.ambientTz2
        } else {
            formatLine(tz, tz.ambientSecondTimezone, tz.ambientSecondLabel, now)
        }
        return tz1 to tz2
    }

    private fun String.isValid(): Boolean = isNotBlank() && !startsWith("--")

    private fun formatLine(
        tz: TimezoneConfig,
        zoneId: String,
        label: String,
        now: ZonedDateTime,
    ): String {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrElse { ZoneId.systemDefault() }
        return "$label  ${now.withZoneSameInstant(zone).format(timeFmt)}"
    }
}
