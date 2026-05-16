package com.watchapp.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.watchapp.data.FaceDataRefresher

class RefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ok = FaceDataRefresher.refresh(applicationContext)
        Log.i(TAG, "Worker finished ok=$ok")
        return if (ok) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "RefreshWorker"

        const val WORK_NAME = "refresh_worker"

        fun oneTimeRequest() = OneTimeWorkRequestBuilder<RefreshWorker>().build()

        fun enqueueNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "refresh_now",
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest(),
            )
        }
    }
}
