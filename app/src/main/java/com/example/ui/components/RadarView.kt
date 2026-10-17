package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.BeaconSignal
import com.example.model.SignalType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.SignalGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarView(
    signals: List<BeaconSignal>,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    onSignalClick: ((BeaconSignal) -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f

            // Concentric rings
            val ringColors = listOf(0.33f, 0.66f, 1.0f)
            ringColors.forEach { ratio ->
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.22f),
                    radius = maxRadius * ratio,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Cross hairs
            drawLine(
                color = CyberCyan.copy(alpha = 0.18f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = CyberCyan.copy(alpha = 0.18f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Scanning pulse ring
            if (isScanning) {
                drawCircle(
                    color = CyberCyan.copy(alpha = (1.0f - pulseRadius) * 0.4f),
                    radius = maxRadius * pulseRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Rotating radar sweep beam
                val sweepRad = Math.toRadians(sweepAngle.toDouble())
                val endX = center.x + (maxRadius * cos(sweepRad)).toFloat()
                val endY = center.y + (maxRadius * sin(sweepRad)).toFloat()

                drawLine(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberCyan, CyberCyan.copy(alpha = 0f)),
                        center = center,
                        radius = maxRadius
                    ),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 2.5.dp.toPx()
                )
            }

            // Center radar hub
            drawCircle(
                color = if (isScanning) SignalGreen else Color.Gray,
                radius = 5.dp.toPx(),
                center = center
            )

            // Plot detected signals as glowing dots
            signals.take(12).forEachIndexed { index, signal ->
                // Map RSSI (-100 to -35) to distance from center (0.9 to 0.15)
                val normalizedDistance = ((signal.rssi + 100).coerceIn(5, 65) / 70f)
                val radialDistance = maxRadius * (1.05f - normalizedDistance).coerceIn(0.18f, 0.92f)

                // Deterministic angle based on identifier hash
                val baseAngle = (signal.identifier.hashCode() % 360).let { if (it < 0) it + 360 else it }
                val angleRad = Math.toRadians(baseAngle.toDouble())

                val dotX = center.x + (radialDistance * cos(angleRad)).toFloat()
                val dotY = center.y + (radialDistance * sin(angleRad)).toFloat()

                val dotColor = if (signal.type == SignalType.BLE) CyberCyan else ElectricIndigo

                // Outer glow
                drawCircle(
                    color = dotColor.copy(alpha = 0.35f),
                    radius = 7.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
                // Inner bright blip
                drawCircle(
                    color = dotColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}
