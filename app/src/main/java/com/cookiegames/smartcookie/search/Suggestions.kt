package com.cookiegames.smartcookie.search

/**
 * The suggestion choices.
 *
 * Created by anthonycr on 2/19/18.
 */
enum class Suggestions(val index: Int) {
    GOOGLE(0),
    DUCK(1),
    BAIDU(2),
    NAVER(3),
    NONE(4),;

    companion object {
        fun from(value: Int): Suggestions {
            return when (value) {
                0 -> GOOGLE
                1 -> DUCK
                2 -> BAIDU
                3 -> NAVER
                4 -> NONE
                else -> GOOGLE
            }
        }
    }
}