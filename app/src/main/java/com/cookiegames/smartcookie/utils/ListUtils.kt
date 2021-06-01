package com.cookiegames.smartcookie.utils

fun stringContainsItemFromList(inputStr: String, items: Array<String>): Boolean {
    for (i in items.indices) {
        if (inputStr.contains(items[i])) {
            return true
        }
    }
    return false
}