package com.watchapp.util

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

object NodeMessaging {
    suspend fun sendToPhone(context: Context, path: String, payload: ByteArray): Boolean {
        val nodeClient = Wearable.getNodeClient(context)
        val nodes = nodeClient.connectedNodes.await()
        val target = nodes.firstOrNull() ?: return false
        Wearable.getMessageClient(context).sendMessage(target.id, path, payload).await()
        return true
    }
}
