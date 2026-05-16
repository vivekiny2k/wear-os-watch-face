package com.watchapp.phone.actions

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.watchapp.shared.ActionMessage
import com.watchapp.shared.ActionResult

class ActionRouter(private val context: Context) {

    fun execute(message: ActionMessage): ActionResult {
        Log.i(TAG, "Executing phone action: ${message.type}/${message.action}")
        TriggerLauncher.execute(context, message)?.let { return it }
        return when (message.type) {
            "camera" -> executeCamera(message.action)
            "media" -> executeMedia(message.action)
            else -> ActionResult(
                ok = false,
                action = message.action,
                message = "Unknown action type: ${message.type}",
            )
        }
    }

    private fun executeCamera(action: String): ActionResult = when (action) {
        "photo" -> {
            val ok = CameraController.launchPhoto(context)
            ActionResult(ok, "photo", if (ok) "Camera opened" else "Camera unavailable")
        }
        "video" -> {
            val ok = CameraController.launchVideo(context)
            ActionResult(ok, "video", if (ok) "Video camera opened" else "Video unavailable")
        }
        "selfie" -> {
            val ok = CameraController.launchPhoto(context)
            ActionResult(ok, "selfie", if (ok) "Camera opened (selfie)" else "Camera unavailable")
        }
        "zoom1", "zoom3", "zoom06", "zoom10" -> ActionResult(
            ok = false,
            action = action,
            message = "Zoom control not implemented on phone yet",
        )
        else -> ActionResult(false, action, "Unknown camera action: $action")
    }

    private fun executeMedia(action: String): ActionResult = when (action) {
        "mic" -> ActionResult(false, action, "Voice trigger not implemented yet")
        else -> ActionResult(false, action, "Unknown media action: $action")
    }

    companion object {
        private const val TAG = "ActionRouter"

        fun showToast(context: Context, result: ActionResult) {
            val text = if (result.ok) result.message else "Failed: ${result.message}"
            Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
        }
    }
}
