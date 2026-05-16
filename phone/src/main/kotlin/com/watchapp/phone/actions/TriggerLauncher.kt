package com.watchapp.phone.actions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.content.pm.LauncherApps
import android.os.Process
import android.util.Log
import com.watchapp.shared.ActionMessage
import com.watchapp.shared.ActionResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object TriggerLauncher {
    private const val TAG = "TriggerLauncher"
    private val json = Json { ignoreUnknownKeys = true }

    fun execute(context: Context, message: ActionMessage): ActionResult? = when (message.type) {
        "app" -> launchApp(context, message)
        "shortcut" -> launchShortcut(context, message)
        "intent" -> launchIntent(context, message)
        else -> null
    }

    private fun launchApp(context: Context, message: ActionMessage): ActionResult {
        val packageName = message.extra["packageName"]?.trim().orEmpty()
        if (packageName.isEmpty()) {
            return ActionResult(false, message.action, "No app package configured")
        }
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return ActionResult(false, message.action, "App not installed: $packageName")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        return ActionResult(true, message.action, "Launched $packageName")
    }

    private fun launchShortcut(context: Context, message: ActionMessage): ActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return ActionResult(false, message.action, "Shortcuts require Android 8+")
        }
        val packageName = message.extra["packageName"]?.trim().orEmpty()
        val shortcutId = message.extra["shortcutId"]?.trim().orEmpty()
        if (packageName.isEmpty() || shortcutId.isEmpty()) {
            return ActionResult(false, message.action, "Set package and shortcut ID")
        }
        val launcherApps = context.getSystemService(LauncherApps::class.java)
            ?: return ActionResult(false, message.action, "LauncherApps unavailable")
        return runCatching {
            launcherApps.startShortcut(
                packageName,
                shortcutId,
                null,
                null,
                Process.myUserHandle(),
            )
            ActionResult(true, message.action, "Ran shortcut $shortcutId")
        }.getOrElse {
            Log.e(TAG, "Shortcut launch failed", it)
            ActionResult(false, message.action, it.message ?: "Shortcut failed")
        }
    }

    private fun launchIntent(context: Context, message: ActionMessage): ActionResult {
        val action = message.extra["intentAction"]?.trim().orEmpty()
        val data = message.extra["intentData"]?.trim().orEmpty()
        val component = message.extra["component"]?.trim().orEmpty()
        val extrasJson = message.extra["extrasJson"]?.trim().orEmpty()

        if (action.isEmpty() && component.isEmpty()) {
            return ActionResult(false, message.action, "Set intent action or component")
        }

        val intent = Intent().apply {
            if (action.isNotEmpty()) this.action = action
            if (data.isNotEmpty()) this.data = Uri.parse(data)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            component.takeIf { it.isNotEmpty() }?.let { raw ->
                val slash = raw.indexOf('/')
                if (slash > 0) {
                    val pkg = raw.substring(0, slash)
                    val cls = raw.substring(slash + 1).removePrefix("/")
                    setComponent(ComponentName(pkg, cls))
                }
            }
            applyExtras(this, extrasJson)
        }

        return runCatching {
            if (intent.component == null && intent.action == null) {
                return ActionResult(false, message.action, "Invalid intent")
            }
            val canResolve = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY,
            ).isNotEmpty() || intent.component != null
            if (!canResolve && intent.action?.startsWith("android.") != true) {
                // Broadcasts / Automate flows may not resolve via query; still try startActivity/sendBroadcast
            }
            when {
                action.contains("broadcast", ignoreCase = true) ||
                    message.extra["delivery"] == "broadcast" -> {
                    context.sendBroadcast(intent)
                    ActionResult(true, message.action, "Broadcast sent")
                }
                else -> {
                    context.startActivity(intent)
                    ActionResult(true, message.action, "Intent started")
                }
            }
        }.getOrElse {
            Log.e(TAG, "Intent launch failed", it)
            ActionResult(false, message.action, it.message ?: "Intent failed")
        }
    }

    private fun applyExtras(intent: Intent, extrasJson: String) {
        if (extrasJson.isBlank()) return
        runCatching {
            val obj = json.parseToJsonElement(extrasJson).jsonObject
            obj.forEach { (key, value) ->
                when {
                    value.jsonPrimitive.isString -> intent.putExtra(key, value.jsonPrimitive.content)
                    value.jsonPrimitive.content == "true" || value.jsonPrimitive.content == "false" ->
                        intent.putExtra(key, value.jsonPrimitive.content.toBoolean())
                    else -> value.jsonPrimitive.content.toLongOrNull()?.let { intent.putExtra(key, it) }
                        ?: value.jsonPrimitive.content.toDoubleOrNull()?.let { intent.putExtra(key, it) }
                }
            }
        }.onFailure { Log.w(TAG, "Could not parse extras JSON", it) }
    }
}
