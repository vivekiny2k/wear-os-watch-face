package com.watchapp.phone.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListSelectorActivity : ComponentActivity() {

    private var rows by mutableStateOf<List<SelectorRow>>(emptyList())
    private var loading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_APPS
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Select"
        val packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()

        lifecycleScope.launch {
            loading = true
            rows = withContext(Dispatchers.IO) {
                when (mode) {
                    MODE_SHORTCUTS -> loadShortcutRows(this@ListSelectorActivity, packageName)
                    MODE_ALL_SHORTCUTS -> loadAllShortcutRows(this@ListSelectorActivity)
                    MODE_APPS_FOR_SHORTCUT -> loadAppRowsForShortcutPick(this@ListSelectorActivity)
                    else -> loadAppRows(this@ListSelectorActivity)
                }
            }
            loading = false
        }

        setContent {
            MaterialTheme {
                ListSelectorScreen(
                    title = title,
                    rows = rows,
                    loading = loading,
                    emptyMessage = when (mode) {
                        MODE_SHORTCUTS, MODE_ALL_SHORTCUTS ->
                            "No shortcuts found for this app. Long-press the app on your home screen and add a shortcut, then try again."
                        MODE_APPS_FOR_SHORTCUT ->
                            "No apps found. Check that WatchApp can see installed apps in system settings."
                        else -> "No launchable apps found."
                    },
                    onBack = { finish() },
                    onSelect = { row ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().apply {
                                putExtra(RESULT_ID, row.id)
                                putExtra(RESULT_LABEL, row.label)
                                putExtra(RESULT_PACKAGE, row.packageName ?: row.id)
                            },
                        )
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_TITLE = "title"
        const val EXTRA_PACKAGE = "package"

        const val RESULT_ID = "id"
        const val RESULT_LABEL = "label"
        const val RESULT_PACKAGE = "package"

        const val MODE_APPS = "apps"
        const val MODE_SHORTCUTS = "shortcuts"
        const val MODE_ALL_SHORTCUTS = "all_shortcuts"
        const val MODE_APPS_FOR_SHORTCUT = "apps_for_shortcut"

        fun appIntent(context: Context, title: String = "Select app"): Intent =
            Intent(context, ListSelectorActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_APPS)
                putExtra(EXTRA_TITLE, title)
            }

        fun shortcutsForPackageIntent(
            context: Context,
            packageName: String,
            title: String = "Select shortcut",
        ): Intent = Intent(context, ListSelectorActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_SHORTCUTS)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_PACKAGE, packageName)
        }

        fun allShortcutsIntent(context: Context, title: String = "Select shortcut"): Intent =
            Intent(context, ListSelectorActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_ALL_SHORTCUTS)
                putExtra(EXTRA_TITLE, title)
            }

        fun appForShortcutIntent(context: Context, title: String = "Select app"): Intent =
            Intent(context, ListSelectorActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_APPS_FOR_SHORTCUT)
                putExtra(EXTRA_TITLE, title)
            }
    }
}

private fun loadAppRows(context: Context): List<SelectorRow> =
    loadLaunchableApps(context).map { app ->
        SelectorRow(
            id = app.packageName,
            label = app.label,
            subtitle = app.packageName,
            icon = app.icon,
            packageName = app.packageName,
        )
    }

private fun loadShortcutRows(context: Context, packageName: String): List<SelectorRow> =
    loadShortcutsForPackage(context, packageName).map { shortcut ->
        SelectorRow(
            id = shortcut.id,
            label = shortcut.label,
            subtitle = packageName,
            icon = shortcut.icon,
            packageName = packageName,
        )
    }

private fun loadAppRowsForShortcutPick(context: Context): List<SelectorRow> =
    loadLaunchableApps(context).map { app ->
        SelectorRow(
            id = app.packageName,
            label = app.label,
            subtitle = "Show shortcuts",
            icon = app.icon,
            packageName = app.packageName,
        )
    }

private fun loadAllShortcutRows(context: Context): List<SelectorRow> =
    loadAllLauncherShortcuts(context).map { shortcut ->
        SelectorRow(
            id = shortcut.id,
            label = shortcut.label,
            subtitle = shortcut.packageName,
            icon = shortcut.icon,
            packageName = shortcut.packageName,
        )
    }
