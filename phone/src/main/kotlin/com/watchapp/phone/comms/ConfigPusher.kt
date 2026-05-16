package com.watchapp.phone.comms

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.watchapp.shared.ButtonConfig
import com.watchapp.shared.ConfigJson
import com.watchapp.shared.ConfigPushBundle
import com.watchapp.shared.DataPaths
import com.watchapp.shared.MessagePaths
import com.watchapp.shared.RefreshConfig
import com.watchapp.shared.TimezoneConfig
import com.watchapp.shared.UnitsConfig
import kotlinx.coroutines.tasks.await

data class PushResult(
    val success: Boolean,
    val message: String,
)

class ConfigPusher(private val context: Context) {
    suspend fun pushAll(
        units: UnitsConfig,
        refresh: RefreshConfig,
        timezone: TimezoneConfig,
        buttons: List<ButtonConfig>,
    ): PushResult {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) {
            Log.w(TAG, "pushAll: no connected watch nodes")
            return PushResult(
                success = false,
                message = "No watch on Wear OS data layer. Open Galaxy Wearable, confirm the watch is paired and synced, then try again.",
            )
        }
        return runCatching {
            val client = Wearable.getDataClient(context)
            put(client, DataPaths.CONFIG_UNITS, ConfigJson.json.encodeToString(UnitsConfig.serializer(), units))
            put(client, DataPaths.CONFIG_REFRESH, ConfigJson.json.encodeToString(RefreshConfig.serializer(), refresh))
            put(client, DataPaths.CONFIG_TIMEZONE, ConfigJson.json.encodeToString(TimezoneConfig.serializer(), timezone))
            put(client, DataPaths.CONFIG_BUTTONS, ConfigJson.encodeButtons(buttons))
            val nodeIds = nodes.map { it.id }
            pushTimezoneMessage(nodeIds, timezone)
            pushConfigMessage(nodeIds, units, refresh, timezone, buttons)
            val names = nodes.joinToString { it.displayName }
            Log.i(TAG, "pushAll: synced to $names (data layer + messages)")
            PushResult(success = true, message = "Pushed to watch ($names)")
        }.getOrElse { error ->
            Log.e(TAG, "pushAll failed", error)
            PushResult(success = false, message = error.message ?: "Push failed")
        }
    }

    suspend fun pushStored(repo: com.watchapp.phone.data.PhoneConfigRepository): PushResult =
        pushAll(
            units = repo.loadUnits(),
            refresh = repo.loadRefresh(),
            timezone = repo.loadTimezone(),
            buttons = repo.getButtons(),
        )

    suspend fun pushButtons(buttons: List<ButtonConfig>): PushResult {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) {
            return PushResult(success = false, message = "No watch connected")
        }
        return runCatching {
            put(Wearable.getDataClient(context), DataPaths.CONFIG_BUTTONS, ConfigJson.encodeButtons(buttons))
            PushResult(success = true, message = "Buttons pushed")
        }.getOrElse { PushResult(success = false, message = it.message ?: "Push failed") }
    }

    private suspend fun pushTimezoneMessage(nodeIds: List<String>, timezone: TimezoneConfig) {
        val payload = ConfigJson.json.encodeToString(TimezoneConfig.serializer(), timezone).encodeToByteArray()
        val messageClient = Wearable.getMessageClient(context)
        for (nodeId in nodeIds) {
            messageClient.sendMessage(nodeId, MessagePaths.CONFIG_TIMEZONE, payload).await()
            Log.i(TAG, "Sent timezone message to $nodeId: ${timezone.secondaryTimezone}")
        }
    }

    private suspend fun pushConfigMessage(
        nodeIds: List<String>,
        units: UnitsConfig,
        refresh: RefreshConfig,
        timezone: TimezoneConfig,
        buttons: List<ButtonConfig>,
    ) {
        val payload = ConfigJson.json.encodeToString(
            ConfigPushBundle.serializer(),
            ConfigPushBundle(units, refresh, timezone, buttons),
        ).encodeToByteArray()
        val messageClient = Wearable.getMessageClient(context)
        for (nodeId in nodeIds) {
            messageClient.sendMessage(nodeId, MessagePaths.CONFIG_PUSH, payload).await()
            Log.i(TAG, "Sent config message to node $nodeId (tz=${timezone.secondaryTimezone})")
        }
    }

    private suspend fun put(
        client: com.google.android.gms.wearable.DataClient,
        path: String,
        json: String,
    ) {
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putString("json", json)
            dataMap.putLong("updatedAt", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        client.putDataItem(request).await()
    }

    companion object {
        private const val TAG = "ConfigPusher"
    }
}
