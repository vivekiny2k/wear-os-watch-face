package com.watchapp.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.watchapp.data.DataRefreshPolicy
import java.util.concurrent.TimeUnit

class RefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val refreshed = DataRefreshPolicy.refreshIfDue(applicationContext, "worker")
        Log.i(TAG, "Worker finished refreshed=$refreshed")
        return Result.success()
    }

    companion object {
        private const val TAG = "RefreshWorker"

        const val WORK_NAME = "refresh_worker"
        private const val ONE_SHOT_NAME = "refresh_now"

        fun reschedulePeriodic(context: Context) {
            val minutes = DataRefreshPolicy.intervalMinutes(context)
                .coerceIn(WORK_MANAGER_MIN_MINUTES, 360)
                .toLong()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(minutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
            Log.i(TAG, "Scheduled periodic refresh every ${minutes}m")
        }

        fun enqueueIfDue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_SHOT_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<RefreshWorker>().build(),
            )
        }

        fun cancelPending(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(ONE_SHOT_NAME)
        }

        /** WorkManager minimum interval for periodic work. */
        private const val WORK_MANAGER_MIN_MINUTES = 15
    }
}
