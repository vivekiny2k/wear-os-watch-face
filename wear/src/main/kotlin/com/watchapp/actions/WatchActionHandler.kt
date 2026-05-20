package com.watchapp.actions

import android.content.Context
import android.widget.Toast
import com.watchapp.data.ConfigRepository
import com.watchapp.data.DataRefreshPolicy
import com.watchapp.data.FlightModeStore
import com.watchapp.shared.ActionResult
import com.watchapp.shared.ButtonConfig
import com.watchapp.shared.WatchLayout
import com.watchapp.watchface.WatchFaceInvalidate
import com.watchapp.watchface.WatchLayoutState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object WatchActionHandler {
    fun execute(context: Context, button: ButtonConfig): ActionResult {
        return when (button.target) {
            com.watchapp.shared.ActionTarget.WATCH -> when (button.action) {
                "refresh" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        DataRefreshPolicy.refreshIfDue(context.applicationContext, "manual", force = true)
                    }
                    ActionResult(true, button.action, "Refreshing…")
                }
                "toggle_face" -> toggleLayout(context)
                "flight_start" -> {
                    val app = context.applicationContext
                    val wasTracking = FlightModeStore.get().tracking
                    FlightModeStore.toggle(app)
                    val message = if (wasTracking) "Flight stopped" else "Flight started"
                    ActionResult(true, button.action, message)
                }
                else -> ActionResult(false, button.action, "Unknown watch action")
            }
            else -> ActionResult(false, button.action, "Not a watch action")
        }
    }

    private fun toggleLayout(context: Context): ActionResult {
        val next = if (WatchLayoutState.isFlightLayout()) WatchLayout.REFERENCE else WatchLayout.FLIGHT
        WatchLayoutState.active = next
        runBlocking { ConfigRepository(context).saveFaceMode(next) }
        WatchFaceInvalidate.request()
        val label = if (WatchLayout.isFlight(next)) "Flight layout" else "Default layout"
        return ActionResult(true, "toggle_face", label)
    }

    fun showResult(context: Context, result: ActionResult) {
        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
    }
}
