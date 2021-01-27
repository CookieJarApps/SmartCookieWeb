package com.cookiegames.smartcookie.browser

import com.cookiegames.smartcookie.preference.IntEnum

/**
 * The available proxy choices.
 */
enum class AdBlockChoice(override val value: Int) : IntEnum {
    ELEMENT(0),
    HOSTS(1),
    NONE(2)
}
