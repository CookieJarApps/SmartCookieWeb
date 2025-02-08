package com.cookiegames.smartcookie.permissions

/**
 * Enum class to handle the different states
 * of permissions since the PackageManager only
 * has a granted and denied state.
 */
enum class Permissions {
    GRANTED,
    DENIED,
    NOT_FOUND
}