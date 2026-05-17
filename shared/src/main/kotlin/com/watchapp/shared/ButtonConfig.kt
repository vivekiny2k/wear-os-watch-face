package com.watchapp.shared

import kotlinx.serialization.Serializable

object ActionTarget {
    const val PHONE = "phone"
    const val WATCH = "watch"
}

@Serializable
data class ButtonConfig(
    val id: String,
    val icon: String,
    val label: String,
    val actionType: String,
    val action: String,
    val target: String = ActionTarget.PHONE,
    val trigger: TriggerConfig = TriggerConfig(),
)

@Serializable
data class ButtonConfigList(
    val buttons: List<ButtonConfig> = emptyList(),
)

/** One or more action panels; each inner list is a ring of buttons around the center hub. */
@Serializable
data class ButtonPanels(
    val panels: List<ButtonConfigList> = emptyList(),
)

object DefaultButtons {
    fun panelOne(): List<ButtonConfig> = listOf(
        ButtonConfig("photo", "📷", "Photo", "camera", "photo", ActionTarget.PHONE),
        ButtonConfig("video", "🎬", "Video", "camera", "video", ActionTarget.PHONE),
        ButtonConfig("zoom10", "10X", "10X", "camera", "zoom10", ActionTarget.PHONE),
        ButtonConfig("zoom3", "3X", "3X", "camera", "zoom3", ActionTarget.PHONE),
        ButtonConfig("zoom1", "1X", "1X", "camera", "zoom1", ActionTarget.PHONE),
        ButtonConfig("zoom06", "0.6", "0.6", "camera", "zoom06", ActionTarget.PHONE),
    )

    fun panelTwo(): List<ButtonConfig> = listOf(
        ButtonConfig("mic", "🎤", "Mic", "media", "mic", ActionTarget.PHONE),
        ButtonConfig("refresh", "↻", "Refresh", "watch", "refresh", ActionTarget.WATCH),
    )

    fun allPanels(): List<List<ButtonConfig>> = listOf(panelOne(), panelTwo())

    /** Legacy single-panel list (camera + media). */
    fun flatButtons(): List<ButtonConfig> = panelOne() + panelTwo()
}
