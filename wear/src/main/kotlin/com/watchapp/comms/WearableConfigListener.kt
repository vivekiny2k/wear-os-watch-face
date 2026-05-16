package com.watchapp.comms

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * In-process listener for Wear OS data layer config changes.
 * More reliable than [ConfigListenerService] alone on some devices.
 */
class WearableConfigListener(
    private val context: Context,
    private val scope: CoroutineScope,
) : DataClient.OnDataChangedListener {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val events = mutableListOf<Pair<String, String>>()
        try {
            for (event in dataEvents) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                val path = ConfigSync.normalizePath(event.dataItem.uri.path) ?: continue
                if (!path.startsWith("/config")) continue
                val json = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("json") ?: continue
                events.add(path to json)
            }
        } finally {
            dataEvents.release()
        }
        if (events.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            var applied = 0
            for ((path, json) in events) {
                Log.i(TAG, "Data changed: $path")
                if (ConfigSync.applyJson(context, path, json)) applied++
            }
            if (applied > 0) {
                ConfigSync.scheduleRefresh(context)
            }
        }
    }

    companion object {
        private const val TAG = "WearableConfigListener"
    }
}
