package com.cookiegames.smartcookie.browser

import com.cookiegames.smartcookie.preference.IntEnum

/**
 * Swap between the 3 default homepage modes
 */
enum class HomepageTypeChoice(override val value: Int) : IntEnum {
    DEFAULT(0),
    FOCUSED(1),
    INFORMATIVE(2)
}
