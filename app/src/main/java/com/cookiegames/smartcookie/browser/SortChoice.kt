package com.cookiegames.smartcookie.browser

import com.cookiegames.smartcookie.preference.IntEnum

/**
 * The available sort types.
 */
enum class SortChoice(override val value: Int) : IntEnum {
    NONE(0),
    A_Z(1),
    Z_A(2),
    DATE(3)
}
