package com.watchapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.watchapp.actions.WatchActionHandler
import com.watchapp.comms.ActionEvents
import com.watchapp.data.ConfigRepository
import com.watchapp.shared.ActionJson
import com.watchapp.shared.ActionTarget
import com.watchapp.shared.ButtonConfig
import com.watchapp.shared.toActionMessage
import com.watchapp.shared.DefaultButtons
import com.watchapp.shared.MessagePaths
import com.watchapp.comms.ConfigSync
import com.watchapp.data.FaceDataRefresher
import com.watchapp.util.LocationPermission
import com.watchapp.util.NodeMessaging
import kotlinx.coroutines.launch

class ActionPanelActivity : ComponentActivity() {
    private val configRepo by lazy { ConfigRepository(this) }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) {
            lifecycleScope.launch { FaceDataRefresher.refresh(this@ActionPanelActivity) }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            ConfigSync.bootstrapFromDataLayer(this@ActionPanelActivity)
            ConfigSync.reloadWatchFaceTimezone(this@ActionPanelActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (LocationPermission.hasPermission(this)) {
            lifecycleScope.launch { FaceDataRefresher.refresh(this@ActionPanelActivity) }
        } else {
            locationPermissionLauncher.launch(LocationPermission.required)
        }
        setContent {
            var buttons by remember { mutableStateOf(DefaultButtons.panelOne()) }
            LaunchedEffect(Unit) {
                buttons = configRepo.getButtons()
            }
            LaunchedEffect(Unit) {
                ActionEvents.messages.collect { msg ->
                    Toast.makeText(this@ActionPanelActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
            MaterialTheme {
                ActionPanelScreen(
                    buttons = buttons,
                    onDismiss = { finish() },
                    onButton = { handleButton(it) },
                )
            }
        }
    }

    private fun handleButton(button: ButtonConfig) {
        lifecycleScope.launch {
            when (button.target) {
                ActionTarget.WATCH -> {
                    val result = WatchActionHandler.execute(this@ActionPanelActivity, button)
                    WatchActionHandler.showResult(this@ActionPanelActivity, result)
                    if (result.ok) {
                        ActionEvents.emit(result.message)
                    }
                }
                else -> {
                    val message = button.toActionMessage()
                    val ok = NodeMessaging.sendToPhone(
                        this@ActionPanelActivity,
                        MessagePaths.ACTION,
                        ActionJson.encode(message),
                    )
                    if (!ok) {
                        Toast.makeText(
                            this@ActionPanelActivity,
                            "No phone connected",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }
}

private val buttonPositions = mapOf(
    "photo" to Pair(68, 24),
    "video" to Pair(130, 24),
    "zoom10" to Pair(24, 72),
    "zoom3" to Pair(68, 72),
    "zoom1" to Pair(130, 72),
    "zoom06" to Pair(174, 72),
    "mic" to Pair(68, 130),
    "selfie" to Pair(130, 130),
    "refresh" to Pair(24, 130),
)

@Composable
private fun ActionPanelScreen(
    buttons: List<ButtonConfig>,
    onDismiss: () -> Unit,
    onButton: (ButtonConfig) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A4A4A), Color(0xFF0D2B2B)),
                ),
            ),
    ) {
        buttons.forEachIndexed { index, button ->
            val pos = buttonPositions[button.id] ?: fallbackPosition(index)
            ActionButton(
                label = button.label,
                emoji = button.icon,
                modifier = Modifier.offset(pos.first.dp, pos.second.dp),
                onClick = { scope.launch { onButton(button) } },
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-28).dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8A0A0))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Dismiss",
                color = Color(0xFF2244AA),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun fallbackPosition(index: Int): Pair<Int, Int> {
    val col = index % 3
    val row = index / 3
    return Pair(24 + col * 56, 24 + row * 56)
}

@Composable
private fun ActionButton(
    label: String,
    emoji: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A1A2E))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Text(text = label, color = Color(0xFF7A90B0), fontSize = 7.sp)
    }
}
