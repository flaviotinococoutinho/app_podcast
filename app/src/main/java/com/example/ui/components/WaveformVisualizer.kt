package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberPodcast
import com.example.ui.theme.ElectricCyan
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    height: Dp = 48.dp,
    primaryColor: Color = AmberPodcast,
    secondaryColor: Color = ElectricCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val totalSpacing = width / barCount
        val barWidth = (totalSpacing * 0.55f).coerceAtLeast(2.5f)

        val gradient = Brush.verticalGradient(
            colors = listOf(primaryColor, secondaryColor)
        )

        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / barCount
            // Symmetric bell curve envelope for deep vocal resonance
            val envelope = (1f - kotlin.math.abs(normalizedIndex - 0.5f) * 1.6f).coerceIn(0.2f, 1.0f)

            val baseFactor = if (isPlaying) {
                val wave1 = kotlin.math.sin((normalizedIndex * 4 * Math.PI + phase * 6).toFloat())
                val wave2 = kotlin.math.cos((normalizedIndex * 2 * Math.PI - phase * 4).toFloat())
                val animatedVal = (wave1 * 0.5f + wave2 * 0.5f + 1f) / 2f
                animatedVal * envelope
            } else {
                (kotlin.math.sin(normalizedIndex * Math.PI).toFloat() * 0.35f) * envelope
            }

            val barHeight = (canvasHeight * baseFactor).coerceIn(canvasHeight * 0.12f, canvasHeight * 0.95f)
            val left = i * totalSpacing + (totalSpacing - barWidth) / 2f
            val top = (canvasHeight - barHeight) / 2f

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
