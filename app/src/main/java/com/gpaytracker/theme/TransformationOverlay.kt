package com.gpaytracker.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class TransformState {
    IDLE, FLASHING, SHOWING_ALIEN, DONE
}

@Composable
fun TransformationOverlay(
    alien: Alien,
    state: TransformState,
    onDone: () -> Unit
) {
    if (state == TransformState.IDLE || state == TransformState.DONE) return

    // Flash alpha — pulses bright then fades
    val flashAlpha by animateFloatAsState(
        targetValue = when (state) {
            TransformState.FLASHING     -> 1f
            TransformState.SHOWING_ALIEN -> 0.85f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "flash"
    )

    // Alien symbol scale — starts huge then settles
    val symbolScale by animateFloatAsState(
        targetValue = when (state) {
            TransformState.FLASHING      -> 0.3f
            TransformState.SHOWING_ALIEN -> 1f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 120f),
        label = "scale"
    )

    val omnitrixGreen = Color(0xFF00FF44)
    val alienColor = alien.primaryColor

    // Pulsing ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(flashAlpha)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        alienColor.copy(alpha = 0.95f),
                        omnitrixGreen.copy(alpha = 0.9f),
                        Color(0xFF003300)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulse ring
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .background(
                    Color.White.copy(alpha = 0.05f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )

        // Alien symbol
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(symbolScale)
        ) {
            Text(
                alien.emoji,
                fontSize = 96.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                alien.name.uppercase(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 6.sp
            )
            Text(
                alien.description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }

        // Scanline effect overlay
        repeat(12) { i ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .offset(y = (i * 60 - 360).dp)
                    .background(Color.White.copy(alpha = 0.04f))
            )
        }
    }

    // Auto-advance: after showing alien, call onDone
    LaunchedEffect(state) {
        if (state == TransformState.SHOWING_ALIEN) {
            kotlinx.coroutines.delay(900)
            onDone()
        }
    }
}
