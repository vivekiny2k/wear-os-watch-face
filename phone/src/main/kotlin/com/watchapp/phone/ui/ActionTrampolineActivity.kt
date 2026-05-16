package com.watchapp.phone.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.android.gms.wearable.Wearable
import com.watchapp.phone.actions.ActionRouter
import com.watchapp.shared.ActionJson
import com.watchapp.shared.MessagePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Starts from [PhoneMessageService] so camera / intents run with a foreground activity context
 * (required on recent Android versions for background launch restrictions).
 */
class ActionTrampolineActivity : ComponentActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val payload = intent.getByteArrayExtra(EXTRA_PAYLOAD)
        val watchNodeId = intent.getStringExtra(EXTRA_WATCH_NODE_ID)
        if (payload == null) {
            finish()
            return
        }
        scope.launch {
            val message = runCatching { ActionJson.decode(payload) }.getOrNull()
            if (message == null) {
                finish()
                return@launch
            }
            val result = ActionRouter(this@ActionTrampolineActivity).execute(message)
            watchNodeId?.let { nodeId ->
                runCatching {
                    Wearable.getMessageClient(this@ActionTrampolineActivity)
                        .sendMessage(nodeId, MessagePaths.CONFIRM, ActionJson.encodeResult(result))
                        .await()
                }
            }
            if (!result.ok && message.type == "camera" && message.action == "photo") {
                // keep activity alive briefly if camera failed
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_WATCH_NODE_ID = "watch_node_id"
    }
}
