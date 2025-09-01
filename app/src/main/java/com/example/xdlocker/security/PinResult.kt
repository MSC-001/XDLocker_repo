package com.example.xdlocker.security // Same package as AppLockManagerImpl

sealed class PinResult<out T> {
    data class Success<out T>(val data: T) : PinResult<T>()
    data class Error(val message: String) : PinResult<Nothing>() // Nothing means no successful data
    // Optional: if you need a loading state for asynchronous operations reflected in PinResult
    // object Loading : PinResult<Nothing>()
}