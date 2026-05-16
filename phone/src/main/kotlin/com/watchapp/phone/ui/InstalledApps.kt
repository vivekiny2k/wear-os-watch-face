package com.watchapp.phone.ui

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.Log

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

private const val TAG = "InstalledApps"

fun loadLaunchableApps(context: Context): List<LaunchableApp> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        if (launcherApps != null) {
            val fromLauncher = runCatching {
                launcherApps.getActivityList(null, Process.myUserHandle())
                    .groupBy { it.applicationInfo.packageName }
                    .map { (pkg, activities) ->
                        val activity = activities.first()
                        val density = context.resources.displayMetrics.densityDpi
                        LaunchableApp(
                            packageName = pkg,
                            label = activity.label?.toString() ?: pkg,
                            icon = runCatching {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    activity.getBadgedIcon(density)
                                } else {
                                    context.packageManager.getApplicationIcon(pkg)
                                }
                            }.getOrNull(),
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            }.onFailure { Log.w(TAG, "LauncherApps.getActivityList failed", it) }
                .getOrDefault(emptyList())
            if (fromLauncher.isNotEmpty()) return fromLauncher
        }
    }
    return loadLaunchableAppsViaPackageManager(context.packageManager)
}

/** Fallback when LauncherApps is empty (should be rare). Do not use MATCH_DEFAULT_ONLY — it hides launcher entries. */
private fun loadLaunchableAppsViaPackageManager(packageManager: PackageManager): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  @Suppress("DEPRECATION")
    val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0L),
        )
    } else {
        packageManager.queryIntentActivities(intent, 0)
    }
    return activities
        .mapNotNull { info ->
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            val label = info.loadLabel(packageManager)?.toString() ?: pkg
            val icon = runCatching { packageManager.getApplicationIcon(pkg) }.getOrNull()
            LaunchableApp(pkg, label, icon)
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
