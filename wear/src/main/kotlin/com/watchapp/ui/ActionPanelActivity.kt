package com.watchapp.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.watchapp.comms.ConfigSync
import com.watchapp.data.ConfigRepository
import com.watchapp.data.FaceDataRefresher
import com.watchapp.shared.ActionJson
import com.watchapp.shared.ActionTarget
import com.watchapp.shared.ButtonConfig
import com.watchapp.shared.DefaultButtons
import com.watchapp.shared.MessagePaths
import com.watchapp.shared.toActionMessage
import com.watchapp.util.LocationPermission
import com.watchapp.util.NodeMessaging
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ActionPanelActivity : ComponentActivity() {
    private val configRepo by lazy { ConfigRepository(this) }
    private var screenOffReceiver: BroadcastReceiver? = null

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
            var panels by remember { mutableStateOf(DefaultButtons.allPanels()) }
            var panelIndex by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                panels = configRepo.getPanels()
            }
            LaunchedEffect(Unit) {
                ActionEvents.messages.collect { msg ->
                    Toast.makeText(this@ActionPanelActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
            MaterialTheme {
                ActionPanelScreen(
                    panels = panels,
                    panelIndex = panelIndex,
                    onPanelIndexChange = { panelIndex = it },
                    onDismiss = { finish() },
                    onButton = { handleButton(it) },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) finish()
            }
        }
        screenOffReceiver = receiver
        registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStop() {
        screenOffReceiver?.let { unregisterReceiver(it) }
        screenOffReceiver = null
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isInteractive) finish()
        super.onStop()
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

private val ButtonSize = 48.dp
private val HubSize = 56.dp
private val OrbitRadius = 72.dp

@Composable
private fun ActionPanelScreen(
    panels: List<List<ButtonConfig>>,
    panelIndex: Int,
    onPanelIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onButton: (ButtonConfig) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val buttons = panels.getOrElse(panelIndex) { emptyList() }
    val panelCount = panels.size.coerceAtLeast(1)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A4A4A), Color(0xFF0D2B2B)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val count = buttons.size.coerceAtLeast(1)
        buttons.forEachIndexed { index, button ->
            val angle = (2.0 * PI * index / count) - (PI / 2.0)
            val dx = (OrbitRadius.value * cos(angle)).dp
            val dy = (OrbitRadius.value * sin(angle)).dp
            Box(
                modifier = Modifier
                    .offset(x = dx, y = dy)
                    .size(ButtonSize),
                contentAlignment = Alignment.Center,
            ) {
                ActionButton(
                    emoji = button.icon,
                    onClick = { scope.launch { onButton(button) } },
                )
            }
        }

        CenterHub(
            panelIndex = panelIndex,
            panelCount = panelCount,
            onClick = {
                if (panelCount <= 1 || panelIndex >= panelCount - 1) {
                    onDismiss()
                } else {
                    onPanelIndexChange(panelIndex + 1)
                }
            },
        )
    }
}

@Composable
private fun CenterHub(
    panelIndex: Int,
    panelCount: Int,
    onClick: () -> Unit,
) {
    val (primary, secondary) = when {
        panelCount <= 1 -> "⌂" to "face"
        panelIndex < panelCount - 1 -> "${panelIndex + 1}" to "next"
        else -> "⌂" to "face"
    }
    Box(
        modifier = Modifier
            .size(HubSize)
            .clip(CircleShape)
            .background(Color(0xFF1A1A2E).copy(alpha = 0.95f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8A0A0),
                textAlign = TextAlign.Center,
            )
            Text(
                text = secondary,
                fontSize = 9.sp,
                color = Color(0xFF7A90B0),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActionButton(
    emoji: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Color(0xFF1A1A2E))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            fontSize = if (emoji.length > 2) 14.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE8E8F0),
            textAlign = TextAlign.Center,
        )
    }
}
