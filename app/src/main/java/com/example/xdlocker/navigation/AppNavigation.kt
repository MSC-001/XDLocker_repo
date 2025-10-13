package com.example.xdlocker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.xdlocker.data.entities.UserDatabaseInfo
import com.example.xdlocker.services.AppLockStateService
import com.example.xdlocker.ui.screens.*
import com.example.xdlocker.viewmodel.SettingsViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    startDestination: String,
    appLockStateService: AppLockStateService
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavigationRoutes.SPLASH_SCREEN) {
            SplashScreen(
                navController = navController,
                settingsViewModel = settingsViewModel,
                appLockStateService = appLockStateService
            )
        }

        composable(NavigationRoutes.APP_LOCK_SCREEN) {
            AppLockScreen(
                onUnlockSuccess = {
                    navController.navigate(NavigationRoutes.DATABASE_LIST) {
                        popUpTo(NavigationRoutes.APP_LOCK_SCREEN) { inclusive = true }
                    }
                    appLockStateService.unlockApp()
                }
            )
        }

        composable(NavigationRoutes.DATABASE_LIST) {
            DatabaseListScreen(
                onNavigateToCreateDatabase = { navController.navigate(NavigationRoutes.CREATE_DATABASE) },
                onNavigateToPasswordList = { dbInfo ->
                    navController.navigate(
                        NavigationRoutes.passwordList(
                            dbId = dbInfo.id,
                            dbLabel = dbInfo.databaseLabel,
                            dbFilename = dbInfo.databaseFilename
                        )
                    )
                },
                onNavigateToSettings = { navController.navigate(NavigationRoutes.SETTINGS) }
            )
        }

        composable(
            route = NavigationRoutes.PASSWORD_LIST_ROUTE_PATTERN,
            arguments = listOf(
                navArgument(NavigationArgs.DATABASE_ID) { type = NavType.IntType },
                navArgument(NavigationArgs.DATABASE_LABEL) { type = NavType.StringType },
                navArgument(NavigationArgs.DATABASE_FILENAME) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val databaseIdArg = backStackEntry.arguments?.getInt(NavigationArgs.DATABASE_ID) ?: 0
            val databaseLabelArg = backStackEntry.arguments?.getString(NavigationArgs.DATABASE_LABEL) ?: ""
            val databaseFilenameArg = backStackEntry.arguments?.getString(NavigationArgs.DATABASE_FILENAME) ?: ""

            val currentDatabaseInfo = UserDatabaseInfo(
                id = databaseIdArg,
                databaseLabel = databaseLabelArg,
                databaseFilename = databaseFilenameArg
            )

            PasswordListScreen(
                databaseInfo = currentDatabaseInfo,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddPassword = {
                    navController.navigate(NavigationRoutes.addPassword(databaseFilename = currentDatabaseInfo.databaseFilename))
                },
                onNavigateToEditPassword = { entryId, databaseFilename ->
                    navController.navigate(NavigationRoutes.editPassword(entryId = entryId, databaseFilename = databaseFilename))
                }
            )
        }

        composable(NavigationRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAppLockSetup = { navController.navigate(NavigationRoutes.APP_LOCK_SETUP_SCREEN) }
            )
        }

        composable(NavigationRoutes.APP_LOCK_SETUP_SCREEN) {
            AppLockSetupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavigationRoutes.CREATE_DATABASE) {
            CreateDatabaseScreen(
                onNavigateBack = { navController.popBackStack() },
                onDatabaseCreated = { dbInfo ->
                    navController.navigate(
                        NavigationRoutes.passwordList(
                            dbId = dbInfo.id,
                            dbLabel = dbInfo.databaseLabel,
                            dbFilename = dbInfo.databaseFilename
                        )
                    ) {
                        popUpTo(NavigationRoutes.CREATE_DATABASE) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavigationRoutes.ADD_PASSWORD_ROUTE_PATTERN,
            arguments = listOf(
                navArgument(NavigationArgs.DATABASE_FILENAME) { type = NavType.StringType; nullable = false }
            )
        ) { 
            AddEditPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavigationRoutes.EDIT_PASSWORD_ROUTE_PATTERN,
            arguments = listOf(
                navArgument(NavigationArgs.ENTRY_ID) { type = NavType.IntType },
                navArgument(NavigationArgs.DATABASE_FILENAME) { type = NavType.StringType } // Add filename argument
            )
        ) { 
            AddEditPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavigationRoutes.CHANGE_PASSWORD_ROUTE_PATTERN,
            arguments = listOf(navArgument(NavigationArgs.DATABASE_ID) { type = NavType.IntType })
        ) { backStackEntry ->
            val databaseIdFromNav = backStackEntry.arguments?.getInt(NavigationArgs.DATABASE_ID) ?: 0
            ChangeDatabasePasswordScreen(
                databaseId = databaseIdFromNav,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
