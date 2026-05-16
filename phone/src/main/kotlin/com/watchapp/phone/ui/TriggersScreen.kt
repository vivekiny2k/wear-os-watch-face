package com.watchapp.phone.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.watchapp.shared.ActionTarget
import com.watchapp.shared.ButtonConfig
import com.watchapp.shared.TriggerConfig
import com.watchapp.shared.TriggerKind
import com.watchapp.shared.triggerSummary

@Composable
fun TriggersScreen(
    buttons: List<ButtonConfig>,
    onButtonChange: (index: Int, ButtonConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Watch button triggers",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Choose where each button runs (phone/watch) and what it launches: built-in action, app, launcher shortcut, or custom intent (e.g. Automate).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            buttons.forEachIndexed { index, button ->
                TriggerCard(
                    button = button,
                    onChange = { onButtonChange(index, it) },
                )
            }
        }
    }
}

@Composable
private fun TriggerCard(
    button: ButtonConfig,
    onChange: (ButtonConfig) -> Unit,
) {
    val context = LocalContext.current

    val appPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val pkg = data.getStringExtra(ListSelectorActivity.RESULT_PACKAGE) ?: return@rememberLauncherForActivityResult
        onChange(
            button.copy(
                trigger = button.trigger.copy(
                    kind = TriggerKind.APP,
                    packageName = pkg,
                ),
            ),
        )
    }

    val packageShortcutsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val pkg = data.getStringExtra(ListSelectorActivity.RESULT_PACKAGE) ?: return@rememberLauncherForActivityResult
        val id = data.getStringExtra(ListSelectorActivity.RESULT_ID) ?: return@rememberLauncherForActivityResult
        onChange(
            button.copy(
                trigger = button.trigger.copy(
                    kind = TriggerKind.SHORTCUT,
                    packageName = pkg,
                    shortcutId = id,
                ),
            ),
        )
    }

    val shortcutAppPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val pkg = data.getStringExtra(ListSelectorActivity.RESULT_PACKAGE) ?: return@rememberLauncherForActivityResult
        packageShortcutsLauncher.launch(
            ListSelectorActivity.shortcutsForPackageIntent(
                context,
                pkg,
                title = "Select shortcut",
            ),
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${button.icon}  ${button.label}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = button.triggerSummary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "Run on",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = button.target == ActionTarget.PHONE,
                    onClick = { onChange(button.copy(target = ActionTarget.PHONE)) },
                    label = { Text("Phone") },
                )
                FilterChip(
                    selected = button.target == ActionTarget.WATCH,
                    onClick = { onChange(button.copy(target = ActionTarget.WATCH)) },
                    label = { Text("Watch") },
                )
            }

            Text(
                text = "Launch",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                KindChip("Built-in", button.trigger.kind == TriggerKind.BUILTIN) {
                    onChange(button.copy(trigger = TriggerConfig(kind = TriggerKind.BUILTIN)))
                }
                KindChip("App", button.trigger.kind == TriggerKind.APP) {
                    onChange(button.copy(trigger = button.trigger.copy(kind = TriggerKind.APP)))
                }
                KindChip("Shortcut", button.trigger.kind == TriggerKind.SHORTCUT) {
                    onChange(button.copy(trigger = button.trigger.copy(kind = TriggerKind.SHORTCUT)))
                }
                KindChip("Intent", button.trigger.kind == TriggerKind.INTENT) {
                    onChange(button.copy(trigger = button.trigger.copy(kind = TriggerKind.INTENT)))
                }
            }

            when (button.trigger.kind) {
                TriggerKind.BUILTIN -> {
                    Text(
                        text = "Uses preset: ${button.actionType} / ${button.action}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                TriggerKind.APP -> {
                    OutlinedTextField(
                        value = button.trigger.packageName,
                        onValueChange = { pkg ->
                            onChange(button.copy(trigger = button.trigger.copy(packageName = pkg)))
                        },
                        label = { Text("App package") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            appPickerLauncher.launch(
                                ListSelectorActivity.appIntent(context, title = "Select app"),
                            )
                        },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text("Select app")
                    }
                }
                TriggerKind.SHORTCUT -> {
                    if (button.trigger.shortcutId.isNotBlank()) {
                        Text(
                            text = "Selected: ${button.trigger.shortcutId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    OutlinedTextField(
                        value = button.trigger.packageName,
                        onValueChange = { pkg ->
                            onChange(button.copy(trigger = button.trigger.copy(packageName = pkg)))
                        },
                        label = { Text("Shortcut app package") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = button.trigger.shortcutId,
                        onValueChange = { id ->
                            onChange(button.copy(trigger = button.trigger.copy(shortcutId = id)))
                        },
                        label = { Text("Shortcut ID") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            shortcutAppPickerLauncher.launch(
                                ListSelectorActivity.appForShortcutIntent(context, title = "Select app"),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text("Select shortcut")
                    }
                    if (button.trigger.packageName.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                packageShortcutsLauncher.launch(
                                    ListSelectorActivity.shortcutsForPackageIntent(
                                        context,
                                        button.trigger.packageName,
                                        title = "Select shortcut",
                                    ),
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        ) {
                            Text("Shortcuts for ${button.trigger.packageName}")
                        }
                    }
                }
                TriggerKind.INTENT -> {
                    OutlinedTextField(
                        value = button.trigger.intentAction,
                        onValueChange = { v ->
                            onChange(button.copy(trigger = button.trigger.copy(intentAction = v)))
                        },
                        label = { Text("Intent action") },
                        placeholder = { Text("e.g. com.llamacorp.automate.ACTION") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = button.trigger.intentData,
                        onValueChange = { v ->
                            onChange(button.copy(trigger = button.trigger.copy(intentData = v)))
                        },
                        label = { Text("Data URI (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = button.trigger.component,
                        onValueChange = { v ->
                            onChange(button.copy(trigger = button.trigger.copy(component = v)))
                        },
                        label = { Text("Component (optional)") },
                        placeholder = { Text("com.app/.Activity") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = button.trigger.extrasJson,
                        onValueChange = { v ->
                            onChange(button.copy(trigger = button.trigger.copy(extrasJson = v)))
                        },
                        label = { Text("Extras JSON (optional)") },
                        placeholder = { Text("""{"flow":"uuid"}""") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun KindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    )
}
