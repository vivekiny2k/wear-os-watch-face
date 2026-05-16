package com.watchapp.comms

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.watchapp.shared.MessagePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WatchMessageService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            MessagePaths.CONFIRM -> {
                val text = messageEvent.data.decodeToString()
                ActionEvents.emit("OK: $text")
            }
            MessagePaths.ERROR -> {
                val text = messageEvent.data.decodeToString()
                ActionEvents.emit("ERR: $text")
            }
            MessagePaths.CONFIG_PUSH -> {
                scope.launch {
                    ConfigPushHandler.apply(this@WatchMessageService, messageEvent.data)
                }
            }
            MessagePaths.CONFIG_TIMEZONE -> {
                scope.launch {
                    ConfigPushHandler.applyTimezone(this@WatchMessageService, messageEvent.data)
                }
            }
        }
    }
}
