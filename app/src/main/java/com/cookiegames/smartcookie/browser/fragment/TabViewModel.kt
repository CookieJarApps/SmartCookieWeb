package com.cookiegames.smartcookie.browser.fragment

/**
 * Created by joeho on 9/8/2017.
 */

import android.graphics.Bitmap

import com.cookiegames.smartcookie.view.LightningView

/**
 * A view model representing the visual state of a tab.
 */
internal class TabViewModel(private val mLightningView: LightningView) {
    val title: String
    val favicon: Bitmap
    val isForegroundTab: Boolean

    init {
        title = mLightningView.title
        favicon = mLightningView.favicon
        isForegroundTab = mLightningView.isForegroundTab
    }

    override fun equals(obj: Any?): Boolean {
        return obj is TabViewModel && obj.mLightningView == mLightningView
    }
}