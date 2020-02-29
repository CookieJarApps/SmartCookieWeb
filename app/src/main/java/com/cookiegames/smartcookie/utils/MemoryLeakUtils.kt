package com.cookiegames.smartcookie.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager

import java.lang.reflect.Field
import java.lang.reflect.Method

object MemoryLeakUtils {

    private val TAG = "MemoryLeakUtils"

    private var sFinishInputLocked: Method? = null

    /**
     * Clears the mNextServedView and mServedView in
     * InputMethodManager and keeps them from leaking.
     *
     * @param application the application needed to get
     * the InputMethodManager that is
     * leaking the views.
     */
    fun clearNextServedView(activity: Activity, application: Application) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            // This shouldn't be a problem on N
            return
        }

        val imm = application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (sFinishInputLocked == null) {
            try {
                sFinishInputLocked = InputMethodManager::class.java.getDeclaredMethod("finishInputLocked")
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "Unable to find method in clearNextServedView", e)
            }

        }

        var isCurrentActivity = false

        try {
            val servedViewField = InputMethodManager::class.java.getDeclaredField("mNextServedView")
            servedViewField.isAccessible = true
            val servedView = servedViewField.get(imm)
            if (servedView is View) {
                isCurrentActivity = servedView.context === activity
            }
        } catch (e: NoSuchFieldException) {
            Log.d(TAG, "Unable to get mNextServedView field", e)
        } catch (e: IllegalAccessException) {
            Log.d(TAG, "Unable to access mNextServedView field", e)
        }

        if (sFinishInputLocked != null && isCurrentActivity) {
            sFinishInputLocked!!.isAccessible = true
            try {
                sFinishInputLocked!!.invoke(imm)
            } catch (e: Exception) {
                Log.d(TAG, "Unable to invoke method in clearNextServedView", e)
            }

        }

    }

    abstract class LifecycleAdapter : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }


}
