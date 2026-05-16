package com.watchapp.phone.ui

import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

data class LauncherShortcut(
    val id: String,
    val label: String,
    val packageName: String,
    val icon: Drawable? = null,
)

private const val TAG = "ShortcutPicker"

fun loadShortcutsForPackage(context: android.content.Context, packageName: String): List<LauncherShortcut> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageName.isBlank()) return emptyList()
    val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
    val pm = context.packageManager
    val density = context.resources.displayMetrics.densityDpi
    val query = LauncherApps.ShortcutQuery().apply {
        setPackage(packageName)
        setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED,
        )
    }
    return runCatching {
        launcherApps.getShortcuts(query, Process.myUserHandle())
            ?.map { info ->
                val icon = runCatching {
                    launcherApps.getShortcutIconDrawable(info, density)
                }.getOrNull() ?: runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
                LauncherShortcut(
                    id = info.id,
                    label = info.shortLabel?.toString()
                        ?: info.longLabel?.toString()
                        ?: info.id,
                    packageName = packageName,
                    icon = icon,
                )
            }
            ?.sortedBy { it.label.lowercase() }
            ?: emptyList()
    }.onFailure { Log.w(TAG, "getShortcuts failed for $packageName", it) }
        .getOrDefault(emptyList())
}

fun loadAllLauncherShortcuts(context: android.content.Context): List<LauncherShortcut> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
    return loadLaunchableApps(context).flatMap { app ->
        loadShortcutsForPackage(context, app.packageName)
    }.sortedBy { it.label.lowercase() }
}

@Composable
fun rememberShortcuts(packageName: String): List<LauncherShortcut> {
    val context = LocalContext.current
    return androidx.compose.runtime.remember(packageName) {
        loadShortcutsForPackage(context, packageName)
    }
}
