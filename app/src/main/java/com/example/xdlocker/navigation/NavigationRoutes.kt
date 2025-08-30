package com.example.xdlocker.navigation

/**
 * Navigation routes for the XDLocker app
 */
object NavigationRoutes {
    const val DATABASE_LIST = "database_list"
    const val CREATE_DATABASE = "create_database"
    const val PASSWORD_LIST = "password_list/{databaseId}/{databaseLabel}/{databaseFilename}"
    const val ADD_PASSWORD = "add_password/{databaseFilename}"
    const val EDIT_PASSWORD = "edit_password/{databaseFilename}/{entryId}"
    const val SETTINGS = "settings"
    const val APP_LOCK_SETUP = "app_lock_setup"
    const val CHANGE_PASSWORD = "change_password/{databaseId}"

    // Route builders
    fun passwordList(databaseId: Int, databaseLabel: String, databaseFilename: String): String {
        return "password_list/$databaseId/$databaseLabel/$databaseFilename"
    }

    fun addPassword(databaseFilename: String): String {
        return "add_password/$databaseFilename"
    }

    fun editPassword(databaseFilename: String, entryId: Int): String {
        return "edit_password/$databaseFilename/$entryId"
    }

    fun changePassword(databaseId: Int): String {
        return "change_password/$databaseId"
    }
}

/**
 * Navigation arguments
 */
object NavigationArgs {
    const val DATABASE_ID = "databaseId"
    const val DATABASE_LABEL = "databaseLabel"
    const val DATABASE_FILENAME = "databaseFilename"
    const val ENTRY_ID = "entryId"
}

/**
 * Navigation destinations sealed class for type safety
 */
sealed class NavigationDestination(val route: String) {
    object DatabaseList : NavigationDestination(NavigationRoutes.DATABASE_LIST)
    object CreateDatabase : NavigationDestination(NavigationRoutes.CREATE_DATABASE)
    object PasswordList : NavigationDestination(NavigationRoutes.PASSWORD_LIST)
    object AddPassword : NavigationDestination(NavigationRoutes.ADD_PASSWORD)
    object EditPassword : NavigationDestination(NavigationRoutes.EDIT_PASSWORD)
    object Settings : NavigationDestination(NavigationRoutes.SETTINGS)
    object AppLockSetup : NavigationDestination(NavigationRoutes.APP_LOCK_SETUP)
    object ChangePassword : NavigationDestination(NavigationRoutes.CHANGE_PASSWORD)
}