package com.watchapp.comms

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.watchapp.shared.MessagePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WearableMessageListener(
    private val context: Context,
    private val scope: CoroutineScope,
) : MessageClient.OnMessageReceivedListener {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.i(TAG, "Message received: ${messageEvent.path} (${messageEvent.data.size} bytes)")
        when (messageEvent.path) {
            MessagePaths.CONFIG_PUSH -> scope.launch {
                ConfigPushHandler.apply(context, messageEvent.data)
            }
            MessagePaths.CONFIG_TIMEZONE -> scope.launch {
                ConfigPushHandler.applyTimezone(context, messageEvent.data)
            }
        }
    }

    companion object {
        private const val TAG = "WearableMessageListener"
    }
}
