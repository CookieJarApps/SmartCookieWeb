package com.cookiegames.smartcookie.utils

object Preconditions {
    /**
     * Ensure that an object is not null
     * and throw a RuntimeException if it
     * is null.
     *
     * @param object check nullness on this object.
     */
    @JvmStatic
    fun checkNonNull(`object`: Any?) {
        if (`object` == null) {
            throw RuntimeException("Object must not be null")
        }
    }
}