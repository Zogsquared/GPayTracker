package com.gpaytracker.theme

import androidx.compose.ui.graphics.Color

data class Alien(
    val name: String,
    val tab: String,
    val emoji: String,               // placeholder icon (real app would use vector drawables)
    val primaryColor: Color,
    val description: String          // shown on the omnitrix dial
)

val ALIENS = listOf(
    Alien(
        name = "Heatblast",
        tab = "dashboard",
        emoji = "🔥",
        primaryColor = Color(0xFFFF6B00),
        description = "Home Base"
    ),
    Alien(
        name = "Four Arms",
        tab = "transactions",
        emoji = "💪",
        primaryColor = Color(0xFFCC2200),
        description = "Transactions"
    ),
    Alien(
        name = "Diamondhead",
        tab = "analytics",
        emoji = "💎",
        primaryColor = Color(0xFF00CCAA),
        description = "Analytics"
    ),
    Alien(
        name = "Wildvine",
        tab = "summaries",
        emoji = "🌿",
        primaryColor = Color(0xFF338833),
        description = "History"
    ),
    Alien(
        name = "Grey Matter",
        tab = "settings",
        emoji = "🧠",
        primaryColor = Color(0xFF6677AA),
        description = "Settings"
    )
)
