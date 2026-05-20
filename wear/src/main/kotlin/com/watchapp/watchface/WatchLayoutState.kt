package com.watchapp.watchface

import com.watchapp.shared.WatchLayout

/** In-memory active layout for the one watch face (reference vs flight grid). */
object WatchLayoutState {
    @Volatile
    var active: String = WatchLayout.REFERENCE

    fun isFlightLayout(): Boolean = WatchLayout.isFlight(active)
}
