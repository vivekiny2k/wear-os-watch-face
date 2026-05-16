package com.watchapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watchapp.shared.AppConfig
import com.watchapp.shared.ButtonConfig
import com.watchapp.shared.ConfigJson
import com.watchapp.shared.DefaultButtons
import com.watchapp.shared.RefreshConfig
import com.watchapp.shared.TimezoneConfig
import com.watchapp.shared.UnitsConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.configDataStore by preferencesDataStore("watch_config")

class ConfigRepository(private val context: Context) {
    private val unitsKey = stringPreferencesKey("units_json")
    private val refreshKey = intPreferencesKey("refresh_minutes")
    private val timezoneKey = stringPreferencesKey("timezone_json")
    private val buttonsKey = stringPreferencesKey("buttons_json")

    fun getConfig(): AppConfig = runBlocking {
        val prefs = context.configDataStore.data.first()
        val units = prefs[unitsKey]?.let {
            runCatching { ConfigJson.json.decodeFromString(UnitsConfig.serializer(), it) }.getOrNull()
        } ?: UnitsConfig()
        val refresh = RefreshConfig(weatherIntervalMinutes = prefs[refreshKey] ?: 30)
        val timezone = prefs[timezoneKey]?.let {
            runCatching { ConfigJson.json.decodeFromString(TimezoneConfig.serializer(), it) }.getOrNull()
        } ?: TimezoneConfig()
        AppConfig(units = units, refresh = refresh, timezone = timezone)
    }

    suspend fun saveUnits(units: UnitsConfig) {
        context.configDataStore.edit {
            it[unitsKey] = ConfigJson.json.encodeToString(UnitsConfig.serializer(), units)
        }
    }

    suspend fun saveRefresh(minutes: Int) {
        context.configDataStore.edit {
            it[refreshKey] = minutes
        }
    }

    suspend fun saveTimezone(timezone: TimezoneConfig) {
        context.configDataStore.edit {
            it[timezoneKey] = ConfigJson.json.encodeToString(TimezoneConfig.serializer(), timezone)
        }
    }

    suspend fun getButtons(): List<ButtonConfig> =
        context.configDataStore.data.map { prefs ->
            prefs[buttonsKey]?.let { runCatching { ConfigJson.decodeButtons(it) }.getOrNull() }
        }.first()?.takeIf { it.isNotEmpty() } ?: DefaultButtons.panelOne()

    fun getButtonsBlocking(): List<ButtonConfig> = runBlocking { getButtons() }

    suspend fun saveButtons(buttons: List<ButtonConfig>) {
        context.configDataStore.edit {
            it[buttonsKey] = ConfigJson.encodeButtons(buttons)
        }
    }
}
