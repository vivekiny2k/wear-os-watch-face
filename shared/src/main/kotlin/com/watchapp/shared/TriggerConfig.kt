package com.watchapp.shared

import kotlinx.serialization.Serializable

object TriggerKind {
    /** Uses the button's built-in actionType + action (camera, refresh, etc.). */
    const val BUILTIN = "builtin"
    const val APP = "app"
    const val SHORTCUT = "shortcut"
    const val INTENT = "intent"
}

@Serializable
data class TriggerConfig(
    val kind: String = TriggerKind.BUILTIN,
    /** Launcher package for app / shortcut triggers. */
    val packageName: String = "",
    /** Shortcut ID from the launcher (long-press shortcut → details). */
    val shortcutId: String = "",
    /** Intent action, e.g. Automate flow action. */
    val intentAction: String = "",
    /** Intent data URI string. */
    val intentData: String = "",
    /** Fully-qualified component: com.example/.MyActivity */
    val component: String = "",
    /** JSON object of string extras, e.g. {"flow":"123"} */
    val extrasJson: String = "",
)

fun ButtonConfig.toActionMessage(): ActionMessage = when (trigger.kind) {
    TriggerKind.APP -> ActionMessage(
        type = "app",
        action = action,
        extra = buildMap {
            if (trigger.packageName.isNotBlank()) put("packageName", trigger.packageName)
        },
    )
    TriggerKind.SHORTCUT -> ActionMessage(
        type = "shortcut",
        action = action,
        extra = buildMap {
            if (trigger.packageName.isNotBlank()) put("packageName", trigger.packageName)
            if (trigger.shortcutId.isNotBlank()) put("shortcutId", trigger.shortcutId)
        },
    )
    TriggerKind.INTENT -> ActionMessage(
        type = "intent",
        action = action,
        extra = buildMap {
            if (trigger.intentAction.isNotBlank()) put("intentAction", trigger.intentAction)
            if (trigger.intentData.isNotBlank()) put("intentData", trigger.intentData)
            if (trigger.component.isNotBlank()) put("component", trigger.component)
            if (trigger.extrasJson.isNotBlank()) put("extrasJson", trigger.extrasJson)
        },
    )
    else -> ActionMessage(type = actionType, action = action)
}

fun ButtonConfig.triggerSummary(): String = when (trigger.kind) {
    TriggerKind.APP -> trigger.packageName.ifBlank { "App (not set)" }
    TriggerKind.SHORTCUT -> {
        val pkg = trigger.packageName.ifBlank { "?" }
        val id = trigger.shortcutId.ifBlank { "?" }
        "Shortcut $pkg/$id"
    }
    TriggerKind.INTENT -> trigger.intentAction.ifBlank { trigger.component }.ifBlank { "Intent (not set)" }
    else -> "$actionType/$action"
}
