package com.watchapp.phone.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watchapp.phone.comms.ConfigPusher
import com.watchapp.phone.comms.WatchConnection
import com.watchapp.phone.data.PhoneConfigRepository
import kotlinx.coroutines.delay
import com.watchapp.shared.ActionTarget
import com.watchapp.shared.ButtonConfig
import com.watchapp.shared.DefaultButtons
import com.watchapp.shared.RefreshConfig
import com.watchapp.shared.TimezoneConfig
import com.watchapp.shared.UnitsConfig
import kotlinx.coroutines.launch

class ConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = PhoneConfigRepository(this)
        val pusher = ConfigPusher(this)
        setContent {
            var selectedTab by remember { mutableIntStateOf(0) }
            var useCelsius by remember { mutableStateOf(false) }
            var useKph by remember { mutableStateOf(false) }
            var refreshMinutes by remember { mutableFloatStateOf(30f) }
            var secondaryTz by remember { mutableStateOf("America/New_York") }
            var delLabel by remember { mutableStateOf("DEL") }
            var nycLabel by remember { mutableStateOf("NYC") }
            var nycTz by remember { mutableStateOf("America/New_York") }
            var buttons by remember { mutableStateOf(DefaultButtons.panelOne()) }
            var watchConnected by remember { mutableStateOf(false) }
            var watchStatusText by remember { mutableStateOf("Checking watch connection…") }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                while (true) {
                    val status = WatchConnection.status(this@ConfigActivity)
                    watchConnected = status.connected
                    watchStatusText = if (status.connected) {
                        "Watch connected: ${status.nodeNames.joinToString()}"
                    } else {
                        "Watch not on Wear OS data layer — settings save on phone only until connected"
                    }
                    delay(3000)
                }
            }

            LaunchedEffect(Unit) {
                runCatching {
                    useCelsius = repo.loadUnits().temperatureUnit == "celsius"
                    useKph = repo.loadUnits().windSpeedUnit == "kph"
                    refreshMinutes = repo.loadRefresh().weatherIntervalMinutes.toFloat()
                    val tz = repo.loadTimezone()
                    secondaryTz = tz.secondaryTimezone
                    delLabel = tz.secondaryLabel
                    nycLabel = tz.ambientSecondLabel
                    nycTz = tz.ambientSecondTimezone
                    buttons = repo.getButtons()
                }.onFailure {
                    buttons = DefaultButtons.panelOne()
                }
            }

            MaterialTheme {
                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(
                            "WatchApp Config",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                        Text(
                            watchStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (watchConnected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Watch") })
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Triggers") })
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp),
                        ) {
                            when (selectedTab) {
                                0 -> WatchSettingsTab(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    useCelsius = useCelsius,
                                    onCelsius = { useCelsius = it },
                                    useKph = useKph,
                                    onKph = { useKph = it },
                                    refreshMinutes = refreshMinutes,
                                    onRefresh = { refreshMinutes = it },
                                    secondaryTz = secondaryTz,
                                    onSecondaryTz = { secondaryTz = it },
                                    delLabel = delLabel,
                                    onDelLabel = { delLabel = it },
                                    nycLabel = nycLabel,
                                    onNycLabel = { nycLabel = it },
                                    nycTz = nycTz,
                                    onNycTz = { nycTz = it },
                                )
                                1 -> TriggersScreen(
                                    buttons = buttons,
                                    onButtonChange = { index, updated ->
                                        buttons = buttons.toMutableList().also {
                                            it[index] = updated
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                )
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val units = UnitsConfig(
                                        temperatureUnit = if (useCelsius) "celsius" else "fahrenheit",
                                        windSpeedUnit = if (useKph) "kph" else "mph",
                                    )
                                    val refresh = RefreshConfig(refreshMinutes.toInt())
                                    val timezone = TimezoneConfig(
                                        secondaryTimezone = secondaryTz,
                                        secondaryLabel = delLabel,
                                        ambientSecondLabel = nycLabel,
                                        ambientSecondTimezone = nycTz,
                                    )
                                    repo.saveUnits(units)
                                    repo.saveRefresh(refresh)
                                    repo.saveTimezone(timezone)
                                    repo.saveButtons(buttons)
                                    val result = pusher.pushAll(units, refresh, timezone, buttons)
                                    Toast.makeText(
                                        this@ConfigActivity,
                                        if (result.success) result.message else "Saved on phone. ${result.message}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                        ) {
                            Text("Save & push to watch")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchSettingsTab(
    modifier: Modifier = Modifier,
    useCelsius: Boolean,
    onCelsius: (Boolean) -> Unit,
    useKph: Boolean,
    onKph: (Boolean) -> Unit,
    refreshMinutes: Float,
    onRefresh: (Float) -> Unit,
    secondaryTz: String,
    onSecondaryTz: (String) -> Unit,
    delLabel: String,
    onDelLabel: (String) -> Unit,
    nycLabel: String,
    onNycLabel: (String) -> Unit,
    nycTz: String,
    onNycTz: (String) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RowSwitch("Celsius", useCelsius, onCelsius)
        RowSwitch("Wind kph", useKph, onKph)
        Text("Refresh interval: ${refreshMinutes.toInt()} min")
        Slider(
            value = refreshMinutes,
            onValueChange = onRefresh,
            valueRange = 15f..60f,
            steps = 8,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = secondaryTz,
            onValueChange = onSecondaryTz,
            label = { Text("Secondary timezone ID") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(value = delLabel, onValueChange = onDelLabel, label = { Text("Ambient TZ1 label") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = nycLabel, onValueChange = onNycLabel, label = { Text("Ambient TZ2 label") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = nycTz, onValueChange = onNycTz, label = { Text("Ambient TZ2 zone ID") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RowSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
