package com.apoorvdarshan.ai.ui.navigation

// ═══════════════════════════════════════════════════════════════════════════
//  FudAI Route constants — single source of truth for all navigation targets
// ═══════════════════════════════════════════════════════════════════════════

object FudAIRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME       = "home"
    const val PROGRESS   = "progress"
    const val COACH      = "coach"
    const val SETTINGS   = "settings"
    const val ABOUT      = "about"

    /** Routes that show the bottom navigation bar. */
    val bottomTabs = listOf(HOME, PROGRESS, COACH, SETTINGS, ABOUT)
}