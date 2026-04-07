package com.gpaytracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.gpaytracker.theme.*
import com.gpaytracker.ui.screens.*
import com.gpaytracker.viewmodel.ExpenseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: ExpenseViewModel, startTab: String = "dashboard") {
    PermissionGateScreen {
        val scope = rememberCoroutineScope()

        // Which alien/tab is currently dialled in (not yet activated)
        var dialIndex by remember {
            mutableStateOf(ALIENS.indexOfFirst { it.tab == startTab }.coerceAtLeast(0))
        }
        // Which alien/tab content is actually showing
        var activeIndex by remember {
            mutableStateOf(ALIENS.indexOfFirst { it.tab == startTab }.coerceAtLeast(0))
        }

        var transformState by remember { mutableStateOf(TransformState.IDLE) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Content area ──────────────────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    when (ALIENS[activeIndex].tab) {
                        "dashboard"    -> DashboardScreen(viewModel)
                        "transactions" -> TransactionsScreen(viewModel)
                        "analytics"    -> AnalyticsScreen(viewModel)
                        "summaries"    -> SummariesScreen(viewModel)
                        "settings"     -> SettingsScreen(viewModel)
                    }
                }

                // ── Omnitrix dial navigator (replaces bottom nav) ─────────────
                OmnitrixNavigator(
                    selectedIndex = dialIndex,
                    onIndexChange = { newIndex ->
                        dialIndex = newIndex
                        // Dialling without activating just previews the alien
                    },
                    onActivate = {
                        if (transformState == TransformState.IDLE && dialIndex != activeIndex) {
                            // Full transformation sequence
                            scope.launch {
                                transformState = TransformState.FLASHING
                                OmnitrixSoundPlayer.playTransformation {
                                    // no-op — we drive state ourselves below
                                }
                                delay(300)
                                transformState = TransformState.SHOWING_ALIEN
                                // onDone fires after 900ms in the overlay
                            }
                        } else if (transformState == TransformState.IDLE && dialIndex == activeIndex) {
                            // Already on this tab — just play a click
                            OmnitrixSoundPlayer.playDialClick()
                        }
                    }
                )
            }

            // ── Transformation flash overlay (on top of everything) ───────────
            TransformationOverlay(
                alien = ALIENS[dialIndex],
                state = transformState,
                onDone = {
                    activeIndex = dialIndex
                    transformState = TransformState.DONE
                    // Small delay before clearing so the fade-out looks clean
                    scope.launch {
                        delay(100)
                        transformState = TransformState.IDLE
                    }
                }
            )
        }
    }
}
