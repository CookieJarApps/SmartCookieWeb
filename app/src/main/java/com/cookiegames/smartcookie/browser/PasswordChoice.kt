package com.cookiegames.smartcookie.browser

import com.cookiegames.smartcookie.preference.IntEnum

/**
 * The available proxy choices.
 */
enum class PasswordChoice(override val value: Int) : IntEnum {
    NONE(0),
    CUSTOM(1)
}
