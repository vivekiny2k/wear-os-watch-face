package com.watchapp.comms

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ConfigListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            val path = ConfigSync.normalizePath(item.uri.path) ?: continue
            val json = DataMapItem.fromDataItem(item).dataMap.getString("json") ?: continue
            scope.launch {
                android.util.Log.i("ConfigListener", "Data changed: $path")
                if (ConfigSync.applyJson(this@ConfigListenerService, path, json)) {
                    ConfigSync.scheduleRefresh(this@ConfigListenerService)
                }
            }
        }
    }
}
