package com.apoorvdarshan.ai.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.apoorvdarshan.ai.AppContainer
import com.apoorvdarshan.ai.services.update.AndroidUpdateChecker
import com.apoorvdarshan.ai.services.update.AndroidUpdateState
import com.apoorvdarshan.ai.ui.about.AboutScreen
import com.apoorvdarshan.ai.ui.coach.CoachScreen
import com.apoorvdarshan.ai.ui.home.HomeScreen
import com.apoorvdarshan.ai.ui.onboarding.OnboardingScreen
import com.apoorvdarshan.ai.ui.progress.ProgressScreen
import com.apoorvdarshan.ai.ui.settings.SettingsScreen

@Composable
fun FudAINavHost(
    container: AppContainer,
    startOnboarding: Boolean,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    
    // Hide the bar while a food analysis is in flight so the AnalyzingOverlay
    // is the only thing on screen.
    val analyzing by container.analyzingFood.collectAsState()
    val showTabs = currentRoute in FudAIRoutes.bottomTabs && !analyzing
    
    val context = LocalContext.current
    val currentVersion = remember(context) { AndroidUpdateChecker.currentVersion(context) }
    var updateAvailable by remember { mutableStateOf(false) }

    LaunchedEffect(currentVersion) {
        updateAvailable = AndroidUpdateChecker.check(currentVersion) is AndroidUpdateState.Available
    }

    // Box allows the Floating Glass NavBar to overlay on top of the screens
    Box(Modifier.fillMaxSize()) {
        
        // 1. App Content (Screens)
        NavHost(
            navController = nav,
            startDestination = if (startOnboarding) FudAIRoutes.ONBOARDING else FudAIRoutes.HOME,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(FudAIRoutes.ONBOARDING) {
                OnboardingScreen(container = container, onComplete = {
                    nav.navigate(FudAIRoutes.HOME) {
                        popUpTo(FudAIRoutes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }
            composable(FudAIRoutes.HOME) { HomeScreen(container = container) }
            composable(FudAIRoutes.PROGRESS) { ProgressScreen(container = container) }
            composable(FudAIRoutes.COACH) { CoachScreen(container = container) }
            composable(FudAIRoutes.SETTINGS) { SettingsScreen(container = container, nav = nav) }
            composable(FudAIRoutes.ABOUT) { AboutScreen(container = container) }
        }

        // 2. Floating Fluid Bottom Nav Bar (Layered on top with slide animation)
        // OPTIMIZATION: Using AnimatedVisibility instead of `if (showTabs)`
        AnimatedVisibility(
            visible = showTabs,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }), // Slides up from bottom edge
            exit = slideOutVertically(targetOffsetY = { it })   // Slides down off screen
        ) {
            FudAIBottomNavBar(
                currentRoute = currentRoute,
                showAboutBadge = updateAvailable,
                onTap = { target ->
                    if (target == currentRoute) return@FudAIBottomNavBar
                    
                    if (target == FudAIRoutes.HOME) {
                        nav.popBackStack(FudAIRoutes.HOME, inclusive = false)
                    } else {
                        nav.navigate(target) {
                            popUpTo(FudAIRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

internal fun NavHostController.current(): String? = currentBackStackEntry?.destination?.route