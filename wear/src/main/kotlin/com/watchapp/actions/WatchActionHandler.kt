package com.watchapp.actions

import android.content.Context
import android.widget.Toast
import com.watchapp.shared.ActionResult
import com.watchapp.shared.ButtonConfig
import com.watchapp.data.FaceDataRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WatchActionHandler {
    fun execute(context: Context, button: ButtonConfig): ActionResult {
        return when (button.actionType) {
            "watch" -> when (button.action) {
                "refresh" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        FaceDataRefresher.refresh(context.applicationContext)
                    }
                    ActionResult(true, button.action, "Refreshing…")
                }
                else -> ActionResult(false, button.action, "Unknown watch action")
            }
            else -> ActionResult(false, button.action, "Not a watch action")
        }
    }

    fun showResult(context: Context, result: ActionResult) {
        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
    }
}
