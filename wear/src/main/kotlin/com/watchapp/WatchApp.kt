package com.watchapp

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.wearable.Wearable
import com.watchapp.comms.ConfigSync
import com.watchapp.comms.WearableConfigListener
import com.watchapp.comms.WearableMessageListener
import com.watchapp.data.FaceDataRefresher
import com.watchapp.data.WatchDataStore
import com.watchapp.shared.MessagePaths
import com.watchapp.util.NodeMessaging
import com.watchapp.sensor.BarometricAltitude
import com.watchapp.workers.RefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WatchApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var configListener: WearableConfigListener? = null
    private var messageListener: WearableMessageListener? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        WatchDataStore.hydrate(this)
        val dataClient = Wearable.getDataClient(this)
        configListener = WearableConfigListener(this, appScope).also { dataClient.addListener(it) }
        messageListener = WearableMessageListener(this, appScope).also {
            Wearable.getMessageClient(this).addListener(it)
        }
        scheduleRefresh()
        appScope.launch {
            ConfigSync.bootstrapFromDataLayer(this@WatchApp)
            ConfigSync.reloadWatchFaceTimezone(this@WatchApp)
            if (NodeMessaging.sendToPhone(this@WatchApp, MessagePaths.CONFIG_REQUEST, ByteArray(0))) {
                Log.i(TAG, "Requested config from phone")
            }
            Log.i(TAG, "Initial data refresh")
            FaceDataRefresher.refresh(this@WatchApp)
        }
        RefreshWorker.enqueueNow(this)
        BarometricAltitude.register(this)
    }

    private fun scheduleRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        private const val TAG = "WatchApp"
        lateinit var instance: WatchApp
            private set
    }
}
