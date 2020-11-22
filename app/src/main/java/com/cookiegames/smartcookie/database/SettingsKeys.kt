package com.cookiegames.smartcookie.database

sealed class SettingsKeys(
        open val key: String,
        open val value: String
)