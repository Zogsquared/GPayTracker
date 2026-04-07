package com.gpaytracker.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun OmnitrixNavigator(
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    onActivate: () -> Unit
) {
    var dragAccumulator by remember { mutableStateOf(0f) }
    val rotationAnim by animateFloatAsState(
        targetValue = selectedIndex * (360f / ALIENS.size),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "rotation"
    )

    val alien = ALIENS[selectedIndex]
    val glowColor by animateColorAsState(
        targetValue = alien.primaryColor,
        animationSpec = tween(400),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A))
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Alien name label above the watch
        Text(
            text = alien.name.uppercase(),
            color = glowColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // The Omnitrix watch face
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                // Scroll gesture: drag left/right to rotate dial
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { dragAccumulator = 0f }
                    ) { _, dragAmount ->
                        dragAccumulator += dragAmount
                        val threshold = 60f
                        if (abs(dragAccumulator) > threshold) {
                            val direction = if (dragAccumulator > 0) -1 else 1
                            val newIndex = (selectedIndex + direction + ALIENS.size) % ALIENS.size
                            OmnitrixSoundPlayer.playDialClick()
                            onIndexChange(newIndex)
                            dragAccumulator = 0f
                        }
                    }
                }
        ) {
            // Outer bezel ring
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF2A2A2A), Color(0xFF111111))
                        )
                    )
            )

            // Rotating dial with alien slots
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .rotate(rotationAnim)
            ) {
                ALIENS.forEachIndexed { i, a ->
                    val angle = (i * (360f / ALIENS.size)) - rotationAnim
                    val isSelected = i == selectedIndex
                    AlienSlotOnDial(
                        alien = a,
                        angle = (i * (360f / ALIENS.size)).toDouble(),
                        isSelected = isSelected,
                        dialSize = 140
                    )
                }
            }

            // Green hourglass symbol in center (Omnitrix logo)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                glowColor.copy(alpha = 0.9f),
                                glowColor.copy(alpha = 0.5f),
                                Color(0xFF003300)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Omnitrix hourglass symbol
                OmnitrixHourglass(color = Color.White)
            }

            // Tap to activate overlay (invisible, just captures taps on center)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.pressed.not() && it.previousPressed }) {
                                    onActivate()
                                }
                            }
                        }
                    }
            )
        }

        Spacer(Modifier.height(6.dp))

        // Description label
        Text(
            text = alien.description,
            color = Color(0x88FFFFFF),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        // Swipe hint dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ALIENS.forEachIndexed { i, a ->
                val isActive = i == selectedIndex
                val dotColor by animateColorAsState(
                    targetValue = if (isActive) glowColor else Color(0x33FFFFFF),
                    label = "dot$i"
                )
                Box(
                    modifier = Modifier
                        .size(if (isActive) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

@Composable
fun AlienSlotOnDial(alien: Alien, angle: Double, isSelected: Boolean, dialSize: Int) {
    val radius = dialSize / 2f - 22f
    val radians = Math.toRadians(angle - 90)
    val x = (radius * Math.cos(radians)).toFloat()
    val y = (radius * Math.sin(radians)).toFloat()

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 0.85f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "scale$angle"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(
                x = (dialSize / 2f + x - 16).dp,
                y = (dialSize / 2f + y - 16).dp
            )
            .size(32.dp)
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) alien.primaryColor.copy(alpha = 0.9f)
                else Color(0xFF1A1A1A)
            )
    ) {
        Text(alien.emoji, fontSize = 14.sp)
    }
}

@Composable
fun OmnitrixHourglass(color: Color) {
    // Stylised hourglass using text — in production replace with a vector drawable
    Text(
        "⧗",
        color = color,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
}
