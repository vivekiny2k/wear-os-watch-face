package com.watchapp.phone.comms

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.watchapp.phone.data.PhoneConfigRepository
import com.watchapp.phone.ui.ActionTrampolineActivity
import com.watchapp.shared.MessagePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhoneMessageService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            MessagePaths.ACTION -> {
                Log.d(TAG, "Action from watch, launching trampoline")
                val intent = Intent(this, ActionTrampolineActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(ActionTrampolineActivity.EXTRA_PAYLOAD, messageEvent.data)
                    putExtra(ActionTrampolineActivity.EXTRA_WATCH_NODE_ID, messageEvent.sourceNodeId)
                }
                startActivity(intent)
            }
            MessagePaths.CONFIG_REQUEST -> {
                Log.i(TAG, "Config request from watch")
                scope.launch {
                    val repo = PhoneConfigRepository(this@PhoneMessageService)
                    val result = ConfigPusher(this@PhoneMessageService).pushStored(repo)
                    Log.i(TAG, "Config push result: ${result.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "PhoneMessageService"
    }
}
