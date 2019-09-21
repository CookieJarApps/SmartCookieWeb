package com.cookiegames.smartcookie.browser

import com.cookiegames.smartcookie.preference.IntEnum

/**
 * The available proxy choices.
 */
enum class SiteBlockChoice(override val value: Int) : IntEnum {
    NONE(0),
    WHITELIST(1),
    BLACKLIST(2)
}
