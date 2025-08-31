package com.example.xdlocker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.xdlocker.navigation.AppNavigation
import com.example.xdlocker.ui.theme.XDLockerTheme
import com.example.xdlocker.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Get a reference to the ViewModel at the Activity level
    private val settingsViewModel: SettingsViewModel by viewModels()

    private var isAppLocked = false
    private var splashScreenVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Keep splash screen visible while loading
        splashScreen.setKeepOnScreenCondition { splashScreenVisible }

        enableEdgeToEdge()
        setContent {
            XDLockerApp(settingsViewModel = settingsViewModel) // Pass the activity's instance
        }

        // Hide splash screen after delay
        lifecycleScope.launch {
            delay(1500) // Show splash for 1.5 seconds
            splashScreenVisible = false
        }
        lifecycleScope.launch {
            settingsViewModel.themeChangedEvent.collect {
                Log.d("MainActivityTheme", "Theme change event received, attempting to recreate Activity.") // ADD THIS LINE
                recreate() // This will recreate the Activity
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if app should be locked
        checkAppLockStatus()
    }

    override fun onPause() {
        super.onPause()
        // Start app lock timer if enabled
        startAppLockTimer()
    }

    private fun checkAppLockStatus() {
        // Check if app lock is enabled and app should be locked
        // This would check SharedPreferences and elapsed time
    }

    private fun startAppLockTimer() {
        // Start timer for auto-lock feature
        // This would use WorkManager or similar for background timing
    }
}

@Composable
fun XDLockerApp(
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    //val navigationState = rememberNavigationState(navController)
    val context = LocalContext.current
    val uiState by settingsViewModel.uiState.collectAsState()

    Log.d("XDLockerAppTheme", "uiState.isDarkTheme from ViewModel: ${uiState.isDarkTheme}") // Log ViewModel state

    XDLockerTheme(darkTheme = uiState.isDarkTheme) {
        // Log the background color that MaterialTheme is providing
        Log.d("XDLockerAppTheme", "MaterialTheme.colorScheme.background: ${MaterialTheme.colorScheme.background}")
        Log.d("XDLockerAppTheme", "MaterialTheme.colorScheme.surface: ${MaterialTheme.colorScheme.surface}")
        Log.d("XDLockerAppTheme", "MaterialTheme.colorScheme.primary: ${MaterialTheme.colorScheme.primary}")


        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(
                navController = navController,
                settingsViewModel = settingsViewModel // This should already be there
            )
        }
    }
}