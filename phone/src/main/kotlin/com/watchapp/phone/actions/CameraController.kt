package com.watchapp.phone.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast

object CameraController {
    private const val TAG = "CameraController"

    fun launchPhoto(context: Context): Boolean = launchCapture(
        context,
        Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        "Camera not available",
    )

    fun launchVideo(context: Context): Boolean = launchCapture(
        context,
        Intent(MediaStore.ACTION_VIDEO_CAPTURE),
        "Video camera not available",
    )

    private fun launchCapture(context: Context, intent: Intent, unavailableMessage: String): Boolean {
        val launch = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolve = launch.resolveActivity(context.packageManager)
        if (resolve == null) {
            Log.w(TAG, unavailableMessage)
            Toast.makeText(context.applicationContext, unavailableMessage, Toast.LENGTH_SHORT).show()
            return false
        }
        return try {
            context.startActivity(launch)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch camera", e)
            Toast.makeText(context.applicationContext, e.message ?: "Launch failed", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /** Use when already inside an Activity (no NEW_TASK flag). */
    fun launchPhoto(activity: Activity): Boolean {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
            true
        } else {
            Toast.makeText(activity, "Camera not available", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
