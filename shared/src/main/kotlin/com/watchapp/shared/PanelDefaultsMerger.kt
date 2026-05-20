package com.watchapp.shared

object PanelDefaultsMerger {
    fun merge(stored: List<List<ButtonConfig>>): List<List<ButtonConfig>> {
        val normalized = normalize(stored)
        val defaults = DefaultButtons.allPanels()
        if (normalized.isEmpty()) return defaults
        val panelCount = maxOf(normalized.size, defaults.size)
        return (0 until panelCount).map { index ->
            mergePanel(
                saved = normalized.getOrElse(index) { emptyList() },
                defaults = defaults.getOrElse(index) { emptyList() },
            )
        }
    }

    private fun normalize(panels: List<List<ButtonConfig>>): List<List<ButtonConfig>> {
        if (panels.size != 1) return panels
        val flat = panels[0]
        if (flat.size <= 6) return panels
        val camera = flat.filter { it.actionType == "camera" }
        val other = flat.filter { it.actionType != "camera" }
        return if (camera.isNotEmpty() && other.isNotEmpty()) listOf(camera, other) else panels
    }

    private fun mergePanel(
        saved: List<ButtonConfig>,
        defaults: List<ButtonConfig>,
    ): List<ButtonConfig> {
        val defaultByAction = defaults.associateBy { it.action }
        val presentActions = saved.map { it.action }.toMutableSet()
        val merged = saved.map { button ->
            when (button.action) {
                "toggle_face" -> defaultByAction["toggle_face"] ?: button.copy(icon = "", label = "Layout")
                "flight_start" -> defaultByAction["flight_start"] ?: button.copy(label = "Start/Stop")
                else -> button
            }
        }.toMutableList()
        for (default in defaults) {
            if (default.action !in presentActions) {
                merged.add(default)
                presentActions.add(default.action)
            }
        }
        return merged
    }

    fun isLayoutToggle(button: ButtonConfig): Boolean = button.action == "toggle_face"

    fun isFlightStart(button: ButtonConfig): Boolean = button.action == "flight_start"
}