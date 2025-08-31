package com.example.xdlocker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.xdlocker.data.entities.UserDatabaseInfo
import com.example.xdlocker.ui.screens.*
import com.example.xdlocker.viewmodel.SettingsViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavigationRoutes.DATABASE_LIST,
    settingsViewModel: SettingsViewModel // Added this parameter
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // Database List Screen
        composable(NavigationRoutes.DATABASE_LIST) {
            DatabaseListScreen(
                onNavigateToCreateDatabase = {
                    navController.navigate(NavigationRoutes.CREATE_DATABASE)
                },
                onNavigateToPasswordList = { database ->
                    val route = NavigationRoutes.passwordList(
                        database.id,
                        database.databaseLabel,
                        database.databaseFilename
                    )
                    navController.navigate(route)
                },
                onNavigateToSettings = {
                    navController.navigate(NavigationRoutes.SETTINGS)
                }
            )
        }

        // Create Database Screen
        composable(NavigationRoutes.CREATE_DATABASE) {
            CreateDatabaseScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDatabaseCreated = { database ->
                    // Navigate back to database list
                    navController.popBackStack()

                    // Optionally navigate directly to the new database
                    // val route = NavigationRoutes.passwordList(
                    //     database.id,
                    //     database.databaseLabel,
                    //     database.databaseFilename
                    // )
                    // navController.navigate(route)
                }
            )
        }

        // Password List Screen
        composable(
            route = NavigationRoutes.PASSWORD_LIST,
            arguments = listOf(
                navArgument(NavigationArgs.DATABASE_ID) { type = NavType.IntType },
                navArgument(NavigationArgs.DATABASE_LABEL) { type = NavType.StringType },
                navArgument(NavigationArgs.DATABASE_FILENAME) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val databaseId = backStackEntry.arguments?.getInt(NavigationArgs.DATABASE_ID) ?: return@composable
            val databaseLabel = backStackEntry.arguments?.getString(NavigationArgs.DATABASE_LABEL) ?: return@composable
            val databaseFilename = backStackEntry.arguments?.getString(NavigationArgs.DATABASE_FILENAME) ?: return@composable

            // Create UserDatabaseInfo from navigation arguments
            val databaseInfo = UserDatabaseInfo(
                id = databaseId,
                databaseLabel = databaseLabel,
                databaseFilename = databaseFilename
            )

            PasswordListScreen(
                databaseInfo = databaseInfo,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddPassword = {
                    val route = NavigationRoutes.addPassword(databaseFilename)
                    navController.navigate(route)
                },
                onNavigateToEditPassword = { entry ->
                    val route = NavigationRoutes.editPassword(databaseFilename, entry.id)
                    navController.navigate(route)
                }
            )
        }

        // Add Password Screen
        composable(
            route = NavigationRoutes.ADD_PASSWORD,
            arguments = listOf(
                navArgument(NavigationArgs.DATABASE_FILENAME) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val databaseFilename = backStackEntry.arguments?.getString(NavigationArgs.DATABASE_FILENAME) ?: return@composable

            AddEditPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Password Screen
        composable(
            route = NavigationRoutes.EDIT_PASSWORD,
            arguments = listOf(
                navArgument(NavigationArgs.DATABASE_FILENAME) { type = NavType.StringType },
                navArgument(NavigationArgs.ENTRY_ID) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val databaseFilename = backStackEntry.arguments?.getString(NavigationArgs.DATABASE_FILENAME) ?: return@composable
            val entryId = backStackEntry.arguments?.getInt(NavigationArgs.ENTRY_ID) ?: return@composable

            AddEditPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(NavigationRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAppLockSetup = { navController.navigate(NavigationRoutes.APP_LOCK_SETUP) },
                viewModel = settingsViewModel // <--- PASS THE VIEWMODEL HERE
            )
        }

        // App Lock Setup Screen
        composable(NavigationRoutes.APP_LOCK_SETUP) {
            AppLockSetupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Change Database Password Screen
        composable(
            route = NavigationRoutes.CHANGE_PASSWORD,
            arguments = listOf(
                navArgument(NavigationArgs.DATABASE_ID) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val databaseId = backStackEntry.arguments?.getInt(NavigationArgs.DATABASE_ID) ?: return@composable

            ChangeDatabasePasswordScreen(
                databaseId = databaseId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Navigation extension functions for easier navigation
 */
fun NavHostController.navigateToPasswordList(database: UserDatabaseInfo) {
    val route = NavigationRoutes.passwordList(
        database.id,
        database.databaseLabel,
        database.databaseFilename
    )
    navigate(route)
}

fun NavHostController.navigateToAddPassword(databaseFilename: String) {
    val route = NavigationRoutes.addPassword(databaseFilename)
    navigate(route)
}

fun NavHostController.navigateToEditPassword(databaseFilename: String, entryId: Int) {
    val route = NavigationRoutes.editPassword(databaseFilename, entryId)
    navigate(route)
}

fun NavHostController.navigateToChangePassword(databaseId: Int) {
    val route = NavigationRoutes.changePassword(databaseId)
    navigate(route)
}

/**
 * Safe navigation that handles back stack properly
 */
fun NavHostController.navigateAndClearBackStack(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            inclusive = true
        }
    }
}

fun NavHostController.navigateWithSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}