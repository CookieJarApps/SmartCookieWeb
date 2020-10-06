package com.cookiegames.smartcookie.browser

import com.cookiegames.smartcookie.preference.IntEnum

/**
 * The available proxy choices.
 */
enum class HomepageChoice(override val value: Int) : IntEnum {
    NONE(0),
    IMAGE(1)
}
