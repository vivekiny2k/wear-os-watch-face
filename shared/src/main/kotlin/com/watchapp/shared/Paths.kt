package com.watchapp.shared

object MessagePaths {
    const val ACTION = "/action"
    const val CONFIRM = "/confirm"
    const val ERROR = "/error"
    const val PING = "/ping"
    const val CONFIG_REQUEST = "/config-request"
    const val CONFIG_PUSH = "/config-push"
    const val CONFIG_TIMEZONE = "/config-timezone"
    const val NOTIFY = "/notify"
}

object DataPaths {
    const val CONFIG_UNITS = "/config/units"
    const val CONFIG_REFRESH = "/config/refresh-intervals"
    const val CONFIG_TIMEZONE = "/config/timezone-secondary"
    const val CONFIG_BUTTONS = "/config/buttons"
}

object ComplicationSlots {
    const val WEATHER_ICON = "SLOT_WEATHER_ICON"
    const val TEMPERATURE = "SLOT_TEMPERATURE"
    const val WIND = "SLOT_WIND"
    const val CONDITIONS = "SLOT_CONDITIONS"
    const val DATE = "SLOT_DATE"
    const val ALT_TIME = "SLOT_ALT_TIME"
    const val SUNRISE = "SLOT_SUNRISE"
    const val SUNSET = "SLOT_SUNSET"
    const val LOCATION = "SLOT_LOCATION"
    const val ALTITUDE = "SLOT_ALTITUDE"
}
