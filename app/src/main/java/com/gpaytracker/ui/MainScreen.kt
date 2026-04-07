package com.gpaytracker.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gpaytracker.theme.*
import com.gpaytracker.ui.screens.*
import com.gpaytracker.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: ExpenseViewModel, startTab: String = "dashboard") {
    PermissionGateScreen {
        val scope = rememberCoroutineScope()

        // Which alien is shown in the content area
        var activeIndex by remember {
            mutableStateOf(ALIENS.indexOfFirst { it.tab == startTab }.coerceAtLeast(0))
        }
        // Which alien is highlighted on the dial (may differ from active while browsing)
        var dialIndex by remember { mutableStateOf(activeIndex) }

        // Whether the full-screen Omnitrix is open
        var omnitrixOpen by remember { mutableStateOf(false) }

        // Transformation flash state
        var transformState by remember { mutableStateOf(TransformState.IDLE) }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {

            // ── Main content ─────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when (ALIENS[activeIndex].tab) {
                    "dashboard"    -> DashboardScreen(viewModel)
                    "transactions" -> TransactionsScreen(viewModel)
                    "analytics"    -> AnalyticsScreen(viewModel)
                    "summaries"    -> SummariesScreen(viewModel)
                    "settings"     -> SettingsScreen(viewModel)
                }
            }

            // ── Persistent Omnitrix button (bottom-right corner) ─────────────
            if (!omnitrixOpen && transformState == TransformState.IDLE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                ) {
                    OmnitrixButton(
                        activeAlien = ALIENS[activeIndex],
                        onClick = {
                            dialIndex = activeIndex  // reset dial to current tab
                            omnitrixOpen = true
                        }
                    )
                }
            }

            // ── Full-screen Omnitrix watch face ──────────────────────────────
            AnimatedVisibility(
                visible = omnitrixOpen,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit  = fadeOut() + scaleOut(targetScale = 0.92f)
            ) {
                OmnitrixFullScreen(
                    dialIndex = dialIndex,
                    onScrollLeft  = {
                        // swipe left = next alien
                        dialIndex = (dialIndex + 1) % ALIENS.size
                    },
                    onScrollRight = {
                        // swipe right = previous alien
                        dialIndex = (dialIndex - 1 + ALIENS.size) % ALIENS.size
                    },
                    onActivate = {
                        omnitrixOpen = false
                        scope.launch {
                            delay(100)
                            transformState = TransformState.FLASHING
                            OmnitrixSoundPlayer.playTransformation {}
                            delay(320)
                            transformState = TransformState.SHOWING_ALIEN
                            // overlay auto-calls onDone after 900ms
                        }
                    },
                    onDismiss = {
                        omnitrixOpen = false
                    }
                )
            }

            // ── Transformation flash overlay ─────────────────────────────────
            TransformationOverlay(
                alien = ALIENS[dialIndex],
                state = transformState,
                onDone = {
                    activeIndex = dialIndex
                    transformState = TransformState.DONE
                    scope.launch {
                        delay(120)
                        transformState = TransformState.IDLE
                    }
                }
            )
        }
    }
}
