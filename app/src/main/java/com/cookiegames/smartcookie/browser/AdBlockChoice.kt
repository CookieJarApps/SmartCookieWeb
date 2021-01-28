package com.cookiegames.smartcookie.browser

import com.cookiegames.smartcookie.preference.IntEnum

/**
 * The available adblock choices.
 */
enum class AdBlockChoice(override val value: Int) : IntEnum {
    HYBRID(0),
    ELEMENT(1),
    HOSTS(2),
    NONE(3)
}
