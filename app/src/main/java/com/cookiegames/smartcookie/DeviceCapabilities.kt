package com.cookiegames.smartcookie

import android.os.Build

/**
 * Capabilities that are specific to certain API levels.
 */
enum class DeviceCapabilities {
    FULL_INCOGNITO,
    WEB_RTC,
    THIRD_PARTY_COOKIE_BLOCKING
}

/**
 * Returns true if the capability is supported, false otherwise.
 */
val DeviceCapabilities.isSupported: Boolean
    get() = when (this) {
        DeviceCapabilities.FULL_INCOGNITO -> Build.VERSION.SDK_INT >= 28
        DeviceCapabilities.WEB_RTC -> Build.VERSION.SDK_INT >= 21
        DeviceCapabilities.THIRD_PARTY_COOKIE_BLOCKING -> Build.VERSION.SDK_INT >= 21
    }