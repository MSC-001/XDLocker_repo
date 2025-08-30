package com.example.xdlocker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Navigation extension functions and utilities
 */

/**
 * Navigate to a destination and clear the back stack
 */
fun NavController.navigateAndClearBackStack(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
        }
    }
}

/**
 * Navigate with single top behavior
 */
fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Navigate with back stack management
 */
fun NavController.navigateWithPopUp(route: String, popUpTo: String, inclusive: Boolean = false) {
    navigate(route) {
        popUpTo(popUpTo) {
            this.inclusive = inclusive
        }
    }
}

/**
 * Safe navigation that checks if destination exists
 */
fun NavController.safeNavigate(route: String) {
    try {
        if (currentDestination?.route != route) {
            navigate(route)
        }
    } catch (e: Exception) {
        // Log error or handle gracefully
    }
}

/**
 * Pop back stack safely
 */
fun NavController.safePopBackStack(): Boolean {
    return try {
        popBackStack()
    } catch (e: Exception) {
        false
    }
}

/**
 * Navigate back or to a specific destination if back stack is empty
 */
fun NavController.navigateBackOr(fallbackRoute: String) {
    if (!safePopBackStack()) {
        navigateAndClearBackStack(fallbackRoute)
    }
}

/**
 * Check if a route is in the back stack
 */
fun NavController.isRouteInBackStack(route: String): Boolean {
    return backQueue.any { it.destination.route == route }
}

/**
 * Get current route safely
 */
fun NavController.getCurrentRoute(): String? {
    return currentDestination?.route
}

/**
 * Navigate with result handling
 */
fun NavController.navigateForResult(route: String, key: String = "result") {
    navigate(route)

    // Listen for result
    currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>(key)
        ?.observeForever { result ->
            if (result != null) {
                // Handle result
                currentBackStackEntry?.savedStateHandle?.remove<String>(key)
            }
        }
}

/**
 * Set result for navigation
 */
fun NavController.setNavigationResult(key: String, result: String) {
    previousBackStackEntry
        ?.savedStateHandle
        ?.set(key, result)
}

/**
 * Composable for handling navigation effects
 */
@Composable
fun NavigationEffect(
    navController: NavController,
    effect: suspend (NavController) -> Unit
) {
    LaunchedEffect(navController) {
        effect(navController)
    }
}

/**
 * Deep link handler
 */
object DeepLinkHandler {

    fun handleDeepLink(navController: NavController, deepLink: String) {
        when {
            deepLink.startsWith("xdlocker://database/") -> {
                val databaseId = deepLink.substringAfterLast("/").toIntOrNull()
                if (databaseId != null) {
                    // Navigate to specific database
                    navController.navigate(NavigationRoutes.DATABASE_LIST)
                }
            }

            deepLink.startsWith("xdlocker://settings") -> {
                navController.navigate(NavigationRoutes.SETTINGS)
            }

            else -> {
                // Default to database list
                navController.navigate(NavigationRoutes.DATABASE_LIST)
            }
        }
    }
}

/**
 * Back handler for different screens
 */
object BackPressHandler {

    fun handleBackPress(
        navController: NavController,
        currentRoute: String?,
        onExitApp: () -> Unit
    ) {
        when (currentRoute) {
            NavigationRoutes.DATABASE_LIST -> {
                // Exit app
                onExitApp()
            }

            NavigationRoutes.PASSWORD_LIST -> {
                // Close database and go back to list
                navController.popBackStack()
            }

            else -> {
                // Default back behavior
                if (!navController.safePopBackStack()) {
                    onExitApp()
                }
            }
        }
    }
}

/**
 * Navigation state management
 */
data class NavigationState(
    val currentRoute: String? = null,
    val canNavigateBack: Boolean = false,
    val backStackEntryCount: Int = 0
)

/**
 * Get navigation state
 */
@Composable
fun rememberNavigationState(navController: NavController): NavigationState {
    var navigationState by remember { mutableStateOf(NavigationState()) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow
            .onEach { backStackEntry ->
                navigationState = NavigationState(
                    currentRoute = backStackEntry.destination.route,
                    canNavigateBack = navController.previousBackStackEntry != null,
                    backStackEntryCount = navController.backQueue.size
                )
            }
            .launchIn(this)
    }

    return navigationState
}

/**
 * Navigation animation configurations
 */
object NavigationAnimations {
    // Slide animations for main navigation
    val slideInFromRight = androidx.compose.animation.slideInHorizontally { it }
    val slideOutToLeft = androidx.compose.animation.slideOutHorizontally { -it }
    val slideInFromLeft = androidx.compose.animation.slideInHorizontally { -it }
    val slideOutToRight = androidx.compose.animation.slideOutHorizontally { it }

    // Fade animations for dialogs
    val fadeIn = androidx.compose.animation.fadeIn()
    val fadeOut = androidx.compose.animation.fadeOut()

    // Scale animations for modal screens
    val scaleIn = androidx.compose.animation.scaleIn()
    val scaleOut = androidx.compose.animation.scaleOut()
}