package com.watchapp.phone.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watchapp.shared.ButtonConfig
import com.watchapp.shared.ConfigJson
import com.watchapp.shared.DefaultButtons
import com.watchapp.shared.RefreshConfig
import com.watchapp.shared.TimezoneConfig
import com.watchapp.shared.UnitsConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.phoneConfigStore by preferencesDataStore("phone_config")

class PhoneConfigRepository(private val context: Context) {
    private val unitsKey = stringPreferencesKey("units_json")
    private val refreshKey = stringPreferencesKey("refresh_json")
    private val timezoneKey = stringPreferencesKey("timezone_json")
    private val buttonsKey = stringPreferencesKey("buttons_json")

    fun getButtonsBlocking(): List<ButtonConfig> = runBlocking {
        context.phoneConfigStore.data.map { prefs ->
            prefs[buttonsKey]?.let { runCatching { ConfigJson.decodeButtons(it) }.getOrNull() }
        }.first() ?: DefaultButtons.panelOne()
    }

    suspend fun getButtons(): List<ButtonConfig> =
        context.phoneConfigStore.data.map { prefs ->
            prefs[buttonsKey]?.let { runCatching { ConfigJson.decodeButtons(it) }.getOrNull() }
        }.first() ?: DefaultButtons.panelOne()

    suspend fun saveButtons(buttons: List<ButtonConfig>) {
        context.phoneConfigStore.edit {
            it[buttonsKey] = ConfigJson.encodeButtons(buttons)
        }
    }

    suspend fun saveUnits(units: UnitsConfig) {
        context.phoneConfigStore.edit {
            it[unitsKey] = ConfigJson.json.encodeToString(UnitsConfig.serializer(), units)
        }
    }

    suspend fun saveRefresh(refresh: RefreshConfig) {
        context.phoneConfigStore.edit {
            it[refreshKey] = ConfigJson.json.encodeToString(RefreshConfig.serializer(), refresh)
        }
    }

    suspend fun saveTimezone(timezone: TimezoneConfig) {
        context.phoneConfigStore.edit {
            it[timezoneKey] = ConfigJson.json.encodeToString(TimezoneConfig.serializer(), timezone)
        }
    }

    suspend fun loadUnits(): UnitsConfig = context.phoneConfigStore.data.map { prefs ->
        prefs[unitsKey]?.let {
            runCatching { ConfigJson.json.decodeFromString(UnitsConfig.serializer(), it) }.getOrNull()
        }
    }.first() ?: UnitsConfig()

    suspend fun loadRefresh(): RefreshConfig = context.phoneConfigStore.data.map { prefs ->
        prefs[refreshKey]?.let {
            runCatching { ConfigJson.json.decodeFromString(RefreshConfig.serializer(), it) }.getOrNull()
        }
    }.first() ?: RefreshConfig()

    suspend fun loadTimezone(): TimezoneConfig = context.phoneConfigStore.data.map { prefs ->
        prefs[timezoneKey]?.let {
            runCatching { ConfigJson.json.decodeFromString(TimezoneConfig.serializer(), it) }.getOrNull()
        }
    }.first() ?: TimezoneConfig()
}
