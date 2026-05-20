package com.watchapp

import android.app.Application
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.watchapp.comms.ConfigSync
import com.watchapp.comms.WearableConfigListener
import com.watchapp.comms.WearableMessageListener
import com.watchapp.data.ConfigRepository
import com.watchapp.data.DataRefreshPolicy
import com.watchapp.data.FlightModeStore
import com.watchapp.data.WatchDataStore
import com.watchapp.receivers.ScreenStateReceiver
import com.watchapp.sensor.BarometricAltitude
import com.watchapp.shared.MessagePaths
import com.watchapp.util.NodeMessaging
import com.watchapp.watchface.WatchLayoutState
import com.watchapp.workers.RefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WatchApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var configListener: WearableConfigListener? = null
    private var messageListener: WearableMessageListener? = null
    private var screenStateReceiver: ScreenStateReceiver? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        WatchDataStore.hydrate(this)
        FlightModeStore.hydrate(this)
        runBlocking {
            WatchLayoutState.active = ConfigRepository(this@WatchApp).getFaceMode()
        }
        val dataClient = Wearable.getDataClient(this)
        configListener = WearableConfigListener(this, appScope).also { dataClient.addListener(it) }
        messageListener = WearableMessageListener(this, appScope).also {
            Wearable.getMessageClient(this).addListener(it)
        }
        registerScreenStateReceiver()
        RefreshWorker.reschedulePeriodic(this)
        appScope.launch {
            ConfigSync.bootstrapFromDataLayer(this@WatchApp)
            ConfigSync.reloadWatchFaceTimezone(this@WatchApp)
            if (NodeMessaging.sendToPhone(this@WatchApp, MessagePaths.CONFIG_REQUEST, ByteArray(0))) {
                Log.i(TAG, "Requested config from phone")
            }
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (pm.isInteractive) {
                BarometricAltitude.register(this@WatchApp)
                DataRefreshPolicy.refreshIfDue(this@WatchApp, "app_start")
            }
        }
    }

    private fun registerScreenStateReceiver() {
        val receiver = ScreenStateReceiver()
        screenStateReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(android.content.Intent.ACTION_SCREEN_ON)
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    companion object {
        private const val TAG = "WatchApp"
        lateinit var instance: WatchApp
            private set
    }
}
