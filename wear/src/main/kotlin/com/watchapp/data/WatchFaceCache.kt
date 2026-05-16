package com.watchapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.faceCacheStore by preferencesDataStore("face_cache")

@Serializable
private data class WatchFaceDataDto(
    val temperature: String,
    val wind: String,
    val windDirectionDegrees: Int? = null,
    val conditions: String,
    val sunrise: String,
    val sunset: String,
    val location: String,
    val altitude: String,
    val ambientTz1: String,
    val ambientTz2: String,
)

class WatchFaceCache(private val context: Context) {
    private val key = stringPreferencesKey("face_data_json")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(data: WatchFaceData) {
        context.faceCacheStore.edit {
            it[key] = json.encodeToString(data.toDto())
        }
    }

    suspend fun load(): WatchFaceData? = context.faceCacheStore.data.map { prefs ->
        prefs[key]?.let { raw ->
            runCatching { json.decodeFromString<WatchFaceDataDto>(raw).toModel() }.getOrNull()
        }
    }.first()

    private fun WatchFaceData.toDto() = WatchFaceDataDto(
        temperature, wind, windDirectionDegrees, conditions, sunrise, sunset, location, altitude, ambientTz1, ambientTz2,
    )

    private fun WatchFaceDataDto.toModel() = WatchFaceData(
        temperature, wind, windDirectionDegrees, conditions, sunrise, sunset, location, altitude, ambientTz1, ambientTz2,
    )
}
