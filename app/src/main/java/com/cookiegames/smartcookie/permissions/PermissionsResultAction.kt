package com.cookiegames.smartcookie.permissions

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.CallSuper
import java.util.Collections

/**
 * This abstract class should be used to create an if/else action that the PermissionsManager
 * can execute when the permissions you request are granted or denied. Simple use involves
 * creating an anonymous instance of it and passing that instance to the
 * requestPermissionsIfNecessaryForResult method. The result will be sent back to you as
 * either onGranted (all permissions have been granted), or onDenied (a required permission
 * has been denied). Ideally you put your functionality in the onGranted method and notify
 * the user what won't work in the onDenied method.
 */
abstract class PermissionsResultAction {
    private val mPermissions: MutableSet<String> = HashSet(1)
    private var mLooper: Looper = Looper.getMainLooper()

    /**
     * Default Constructor
     */
    constructor()

    /**
     * Alternate Constructor. Pass the looper you wish the PermissionsResultAction
     * callbacks to be executed on if it is not the current Looper. For instance,
     * if you are making a permissions request from a background thread but wish the
     * callback to be on the UI thread, use this constructor to specify the UI Looper.
     *
     * @param looper the looper that the callbacks will be called using.
     */
    @Suppress("unused")
    constructor(looper: Looper) {
        mLooper = looper
    }

    /**
     * This method is called when ALL permissions that have been
     * requested have been granted by the user. In this method
     * you should put all your permissions sensitive code that can
     * only be executed with the required permissions.
     */
    abstract fun onGranted()

    /**
     * This method is called when a permission has been denied by
     * the user. It provides you with the permission that was denied
     * and will be executed on the Looper you pass to the constructor
     * of this class, or the Looper that this object was created on.
     *
     * @param permission the permission that was denied.
     */
    abstract fun onDenied(permission: String)

    /**
     * This method is used to determine if a permission not
     * being present on the current Android platform should
     * affect whether the PermissionsResultAction should continue
     * listening for events. By default, it returns true and will
     * simply ignore the permission that did not exist. Usually this will
     * work fine since most new permissions are introduced to
     * restrict what was previously allowed without permission.
     * If that is not the case for your particular permission you
     * request, override this method and return false to result in the
     * Action being denied.
     *
     * @param permission the permission that doesn't exist on this
     * Android version
     * @return return true if the PermissionsResultAction should
     * ignore the lack of the permission and proceed with exection
     * or false if the PermissionsResultAction should treat the
     * absence of the permission on the API level as a denial.
     */
    @Synchronized
    fun shouldIgnorePermissionNotFound(permission: String): Boolean {
        Log.d(
            TAG,
            "Permission not found: $permission"
        )
        return true
    }

    @CallSuper
    @Synchronized
    fun onResult(permission: String, result: Int): Boolean {
        return if (result == PackageManager.PERMISSION_GRANTED) {
            onResult(permission, Permissions.GRANTED)
        } else {
            onResult(permission, Permissions.DENIED)
        }
    }

    /**
     * This method is called when a particular permission has changed.
     * This method will be called for all permissions, so this method determines
     * if the permission affects the state or not and whether it can proceed with
     * calling onGranted or if onDenied should be called.
     *
     * @param permission the permission that changed.
     * @param result     the result for that permission.
     * @return this method returns true if its primary action has been completed
     * and it should be removed from the data structure holding a reference to it.
     */
    @CallSuper
    @Synchronized
    fun onResult(permission: String, result: Permissions): Boolean {
        mPermissions.remove(permission)
        if (result == Permissions.GRANTED) {
            if (mPermissions.isEmpty()) {
                Handler(mLooper).post { onGranted() }
                return true
            }
        } else if (result == Permissions.DENIED) {
            Handler(mLooper).post { onDenied(permission) }
            return true
        } else if (result == Permissions.NOT_FOUND) {
            if (shouldIgnorePermissionNotFound(permission)) {
                if (mPermissions.isEmpty()) {
                    Handler(mLooper).post { onGranted() }
                    return true
                }
            } else {
                Handler(mLooper).post { onDenied(permission) }
                return true
            }
        }
        return false
    }

    /**
     * This method registers the PermissionsResultAction object for the specified permissions
     * so that it will know which permissions to look for changes to. The PermissionsResultAction
     * will then know to look out for changes to these permissions.
     *
     * @param perms the permissions to listen for
     */
    @CallSuper
    @Synchronized
    fun registerPermissions(perms: Array<String>) {
        Collections.addAll(mPermissions, *perms)
    }

    companion object {
        private val TAG: String = PermissionsResultAction::class.java.simpleName
    }
}