package com.watchapp.watchface

import com.watchapp.shared.TimezoneConfig
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

object WatchFaceConfigCache {
    private val timezone = AtomicReference<TimezoneConfig?>(null)

    fun update(config: TimezoneConfig) {
        timezone.set(config)
    }

    fun secondaryZone(): ZoneId {
        val tzId = timezone.get()?.secondaryTimezone
            ?: return ZoneId.of("America/New_York")
        return runCatching { ZoneId.of(tzId) }
            .getOrElse { ZoneId.of("America/New_York") }
    }
}
