package com.gpaytracker.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// ── Small persistent watch button shown in the bottom-right corner ────────────

@Composable
fun OmnitrixButton(
    activeAlien: Alien,
    onClick: () -> Unit
) {
    val glowColor by animateColorAsState(
        targetValue = activeAlien.primaryColor,
        animationSpec = tween(500),
        label = "btnGlow"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "btnPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .scale(pulse)
            .clip(CircleShape)
            .background(Color(0xFF111111))
            .drawBehind {
                drawCircle(
                    color = glowColor.copy(alpha = 0.6f),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            .clickable { onClick() }
    ) {
        // Inner green core
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF00FF44), Color(0xFF007722))
                    )
                )
        ) {
            Text("⧗", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Full-screen Omnitrix watch face ──────────────────────────────────────────

@Composable
fun OmnitrixFullScreen(
    dialIndex: Int,
    onScrollLeft: () -> Unit,      // drag right-to-left → next alien
    onScrollRight: () -> Unit,     // drag left-to-right → previous alien
    onActivate: () -> Unit,        // press centre button
    onDismiss: () -> Unit          // back / close
) {
    val alien = ALIENS[dialIndex]
    val glowColor by animateColorAsState(
        targetValue = alien.primaryColor,
        animationSpec = tween(350),
        label = "fullGlow"
    )

    var dragTotal by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            // Swipe gesture on the whole screen
            .pointerInput(dialIndex) {
                detectHorizontalDragGestures(
                    onDragEnd = { dragTotal = 0f },
                    onDragCancel = { dragTotal = 0f }
                ) { _, amount ->
                    dragTotal += amount
                    val threshold = 80f
                    if (dragTotal > threshold) {          // swiped RIGHT → previous
                        OmnitrixSoundPlayer.playDialClick()
                        onScrollRight()
                        dragTotal = 0f
                    } else if (dragTotal < -threshold) {  // swiped LEFT → next
                        OmnitrixSoundPlayer.playDialClick()
                        onScrollLeft()
                        dragTotal = 0f
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "← BACK",
                    color = Color(0x88FFFFFF),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { onDismiss() }
                )
                Text(
                    "OMNITRIX",
                    color = Color(0xFF00FF44),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
                Text(
                    "${dialIndex + 1}/${ALIENS.size}",
                    color = Color(0x88FFFFFF),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // ── Alien preview card ────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    alien.emoji,
                    fontSize = 100.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    alien.name.uppercase(),
                    color = glowColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 5.sp
                )
                Text(
                    alien.description,
                    color = Color(0x88FFFFFF),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }

            // ── Swipe indicator dots ──────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ALIENS.forEachIndexed { i, _ ->
                    val active = i == dialIndex
                    val dotColor by animateColorAsState(
                        targetValue = if (active) glowColor else Color(0x33FFFFFF),
                        label = "dot$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(if (active) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            // ── Swipe hint text ───────────────────────────────────────────
            Text(
                "← swipe to browse →",
                color = Color(0x44FFFFFF),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            // ── Big centre Omnitrix press button ──────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                // Outer ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                        .drawBehind {
                            drawCircle(
                                color = glowColor.copy(alpha = 0.5f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                ) {
                    // Inner core button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF00FF44), Color(0xFF005522))
                                )
                            )
                            .clickable { onActivate() }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "⧗",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "PRESS",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Press to transform",
                    color = Color(0x66FFFFFF),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
