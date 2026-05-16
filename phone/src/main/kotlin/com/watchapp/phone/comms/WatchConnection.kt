package com.watchapp.phone.comms

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

data class WatchConnectionStatus(
    val connected: Boolean,
    val nodeCount: Int,
    val nodeNames: List<String>,
)

object WatchConnection {
    suspend fun status(context: Context): WatchConnectionStatus {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        return WatchConnectionStatus(
            connected = nodes.isNotEmpty(),
            nodeCount = nodes.size,
            nodeNames = nodes.map { it.displayName },
        )
    }
}
