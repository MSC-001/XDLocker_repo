package com.example.xdlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.xdlocker.navigation.AppNavigation
import com.example.xdlocker.navigation.BackPressHandler
import com.example.xdlocker.navigation.rememberNavigationState
import com.example.xdlocker.ui.theme.XDLockerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
            XDLockerApp()
        }

        // Hide splash screen after delay
        lifecycleScope.launch {
            delay(1500) // Show splash for 1.5 seconds
            splashScreenVisible = false
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
fun XDLockerApp() {
    val navController = rememberNavController()
    val navigationState = rememberNavigationState(navController)
    val context = LocalContext.current

    // Handle system back button
    BackHandler {
        BackPressHandler.handleBackPress(
            navController = navController,
            currentRoute = navigationState.currentRoute,
            onExitApp = {
                (context as? ComponentActivity)?.finish()
            }
        )
    }

    XDLockerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(navController = navController)
        }
    }
}