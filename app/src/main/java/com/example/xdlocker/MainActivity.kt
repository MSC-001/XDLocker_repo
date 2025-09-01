package com.example.xdlocker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme // Keep this for Surface
import androidx.compose.material3.Surface // Keep this
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState // For appStateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.xdlocker.navigation.AppNavigation
import com.example.xdlocker.navigation.NavigationRoutes // Ensure this is your correct import
import com.example.xdlocker.security.AppLockManager
import com.example.xdlocker.services.AppLockStateService
import com.example.xdlocker.ui.theme.XDLockerTheme
import com.example.xdlocker.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest // For themeChangedEvent if still needed
import kotlinx.coroutines.launch
import javax.inject.Inject

// Sealed interface to represent the state of the app initialization
sealed interface AppState {
    object Loading : AppState
    data class Ready(val startDestination: String) : AppState
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var appLockStateService: AppLockStateService

    private val settingsViewModel: SettingsViewModel by viewModels()

    // Use a StateFlow for appState to be collected by Compose
    private val appStateFlow = MutableStateFlow<AppState>(AppState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen() // Install splash screen first
        super.onCreate(savedInstanceState)

        // Keep splash screen visible until appState is Ready
        splashScreen.setKeepOnScreenCondition {
            appStateFlow.value == AppState.Loading
        }

        lifecycleScope.launch {
            // Determine initial route
            val pinConfigured = appLockManager.isPinConfigured()
            val startRoute = if (pinConfigured && !appLockStateService.isCurrentlyUnlocked()) {
                NavigationRoutes.APP_LOCK_SCREEN
            } else {
                // If PIN is configured but already unlocked in session, go to main.
                // If PIN not configured, go to main.
                NavigationRoutes.DATABASE_LIST // ASSUMPTION: Your main screen after unlock or if no lock
            }
            appStateFlow.value = AppState.Ready(startRoute)
            Log.d("MainActivity", "AppState is Ready. Start route: $startRoute")
        }

        enableEdgeToEdge()
        setContent {
            // Collect appStateFlow here
            val currentAppState by appStateFlow.collectAsState()

            XDLockerApp(
                settingsViewModel = settingsViewModel,
                appState = currentAppState,
                appLockStateService = appLockStateService // Pass the service
            )
        }

        // Theme change listener
        lifecycleScope.launch {
            settingsViewModel.themeChangedEvent.collectLatest { // Using collectLatest
                Log.d("MainActivityTheme", "Theme change event received, attempting to recreate Activity.")
                recreate() // Ensure this is the behavior you want
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Lock the app if PIN is configured when app goes to background
        if (::appLockManager.isInitialized && appLockManager.isPinConfigured()) {
            Log.d("MainActivity", "onStop: Locking app.")
            appLockStateService.onAppGoesToBackgroundOrLockNeeded()
        }
    }
}

@Composable
fun XDLockerApp(
    settingsViewModel: SettingsViewModel,
    appState: AppState, // Updated parameter
    appLockStateService: AppLockStateService // New parameter
) {
    val navController = rememberNavController()
    val uiState by settingsViewModel.uiState.collectAsState() // For theme

    XDLockerTheme(darkTheme = uiState.isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (appState) {
                is AppState.Loading -> {
                    // Content displayed while determining start route (splash screen should cover this)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Optional: Show a progress bar if splash screen hiding is too abrupt
                        // CircularProgressIndicator()
                    }
                }
                is AppState.Ready -> {
                    AppNavigation(
                        navController = navController,
                        settingsViewModel = settingsViewModel, // Pass if needed by AppNavigation/screens
                        startDestination = appState.startDestination, // Pass determined startDestination
                        appLockStateService = appLockStateService // Pass the service
                    )
                }
            }
        }
    }
}
