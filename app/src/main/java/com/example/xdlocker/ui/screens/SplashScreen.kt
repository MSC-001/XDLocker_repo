package com.example.xdlocker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.xdlocker.navigation.NavigationRoutes
import com.example.xdlocker.services.AppLockStateService
import com.example.xdlocker.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel, // Parameter is present, can be used later
    appLockStateService: AppLockStateService // Parameter is present, can be used later
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // TODO: Replace with your actual splash screen UI (e.g., Logo)
        Text("Splash Screen")
    }

    LaunchedEffect(key1 = true) {
        delay(2000) // Simulate a 2-second delay
        // Navigate to the App Lock screen, popping up the splash screen from the back stack
        navController.navigate(NavigationRoutes.APP_LOCK_SCREEN) {
            popUpTo(NavigationRoutes.SPLASH_SCREEN) { inclusive = true }
        }
        // More complex logic can be added here to decide the destination based on
        // settingsViewModel or appLockStateService (e.g., if PIN is set, if user is logged in, etc.)
    }
}
