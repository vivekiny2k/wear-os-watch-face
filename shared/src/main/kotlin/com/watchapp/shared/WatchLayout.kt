package com.watchapp.shared

/** UI layout within the single watch face (reference grid vs flight grid). */
object WatchLayout {
    const val REFERENCE = "reference"
    const val FLIGHT = "flight"

    fun isFlight(layout: String): Boolean = layout == FLIGHT
}
