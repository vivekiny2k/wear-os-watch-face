package com.watchapp.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UnitsConfig(
    val temperatureUnit: String = "fahrenheit",
    val windSpeedUnit: String = "mph",
    /** Subtracted from barometric altitude (ft). 0 on ground; set to field elevation when flying. */
    val barometricBaseAltitudeFeet: Int = 0,
)

@Serializable
data class RefreshConfig(
    val weatherIntervalMinutes: Int = 30,
)

@Serializable
data class TimezoneConfig(
    val secondaryTimezone: String = "America/New_York",
    val secondaryLabel: String = "DEL",
    val ambientSecondLabel: String = "NYC",
    val ambientSecondTimezone: String = "America/New_York",
)

@Serializable
data class AppConfig(
    val units: UnitsConfig = UnitsConfig(),
    val refresh: RefreshConfig = RefreshConfig(),
    val timezone: TimezoneConfig = TimezoneConfig(),
    val buttons: List<ButtonConfig> = DefaultButtons.panelOne(),
)

/** Full config payload sent phone → watch via message API (backup to data layer). */
@Serializable
data class ConfigPushBundle(
    val units: UnitsConfig,
    val refresh: RefreshConfig,
    val timezone: TimezoneConfig,
    val buttons: List<ButtonConfig>,
)

object ConfigJson {
    val json = Json { ignoreUnknownKeys = true }

    fun encodeButtons(buttons: List<ButtonConfig>): String =
        json.encodeToString(ButtonConfigList.serializer(), ButtonConfigList(buttons))

    fun decodeButtons(raw: String): List<ButtonConfig> =
        json.decodeFromString(ButtonConfigList.serializer(), raw).buttons
}
