package com.cookiegames.smartcookie.browser

/**
 * A data class for menu items
 * @param id a unique identifier for the item
 * @param name a reference to a string for the item name
 * @param icon a reference to a drawable for the item icon
 * @param enabled whether the item should be shown or not
 * @param divider whether to show a divider line above the menu item or not
 */

data class MenuItemClass(var id: String, var name: Int, var icon: Int, var enabled: Boolean, var divider: Boolean = false)