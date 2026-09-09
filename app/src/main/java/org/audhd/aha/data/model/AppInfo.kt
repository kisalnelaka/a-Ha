package org.audhd.aha.data.model

import android.content.ComponentName
import android.os.UserHandle

/**
 * Lightweight, icon-free representation of an installed launcher activity.
 *
 * Invariant:
 * Strictly eliminates all [android.graphics.Bitmap] and [android.graphics.drawable.Drawable] references
 * to guarantee that idle launcher memory usage remains under the strict 120MB threshold.
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val userHandle: UserHandle? = null,
    val isDistractionApp: Boolean = false,
    val searchIndex: String = (label + " " + packageName).lowercase()
) {
    val componentName: ComponentName
        get() = ComponentName(packageName, activityName)
}
