package com.example.xdlocker.navigation

import android.net.Uri

object NavigationArgs {
    const val DATABASE_ID = "databaseId"
    const val DATABASE_LABEL = "databaseLabel"
    const val DATABASE_FILENAME = "databaseFilename"
    const val ENTRY_ID = "entryId"
}

object NavigationRoutes {
    const val APP_LOCK_SCREEN = "app_lock_screen"
    const val SPLASH_SCREEN = "splash_screen"
    const val DATABASE_LIST = "database_list"
    const val CREATE_DATABASE = "create_database"
    const val SETTINGS = "settings"
    const val APP_LOCK_SETUP_SCREEN = "app_lock_setup_screen"

    // Route patterns for routes that take arguments
    const val PASSWORD_LIST_ROUTE_PATTERN =
        "password_list/{${NavigationArgs.DATABASE_ID}}/{${NavigationArgs.DATABASE_LABEL}}/{${NavigationArgs.DATABASE_FILENAME}}"
    const val ADD_PASSWORD_ROUTE_PATTERN = "add_password/{${NavigationArgs.DATABASE_FILENAME}}"
    const val EDIT_PASSWORD_ROUTE_PATTERN = "edit_password/{${NavigationArgs.ENTRY_ID}}/{${NavigationArgs.DATABASE_FILENAME}}"
    const val CHANGE_PASSWORD_ROUTE_PATTERN = "change_password/{${NavigationArgs.DATABASE_ID}}"

    // Helper functions to build routes with arguments
    fun passwordList(dbId: Int, dbLabel: String, dbFilename: String): String {
        val encodedLabel = Uri.encode(dbLabel)
        val encodedFilename = Uri.encode(dbFilename)
        return "password_list/$dbId/$encodedLabel/$encodedFilename"
    }

    fun addPassword(databaseFilename: String): String {
        val encodedFilename = Uri.encode(databaseFilename)
        return "add_password/$encodedFilename"
    }

    fun editPassword(entryId: Int, databaseFilename: String): String {
        val encodedFilename = Uri.encode(databaseFilename)
        return "edit_password/$entryId/$encodedFilename"
    }

    fun changePassword(databaseId: Int): String {
        return "change_password/$databaseId"
    }
}
