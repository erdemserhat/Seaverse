package com.serhaterdem.seaverse.ui.arpreview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val TAU = (2.0 * PI).toFloat()

@Composable
internal fun UnderwaterCameraFilter(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Layer A: blue water tint
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f   to Color(0x33039BE5),
                    0.5f to Color(0x4400688B),
                    1f   to Color(0x880D1B2A)
                )
            )
        )

        // Layer B: animated caustic light rays
        val transition = rememberInfiniteTransition(label = "caustics")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = TAU,
            animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
            label = "phase"
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(12) { i ->
                val x = size.width * (i / 12f + sin(phase + i) * 0.04f)
                val sweep = size.height * (0.4f + cos(phase * 0.7f + i) * 0.2f)
                drawLine(
                    color = Color.White.copy(alpha = 0.07f),
                    start = Offset(x, 0f),
                    end = Offset(x + sweep * 0.3f, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Layer C: radial vignette
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val radiusPx = with(density) { maxOf(maxWidth, maxHeight).toPx() * 0.75f }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x88061422)),
                        radius = radiusPx
                    )
                )
            )
        }
    }
}
