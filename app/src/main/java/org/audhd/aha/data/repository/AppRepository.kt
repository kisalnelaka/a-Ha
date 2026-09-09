package org.audhd.aha.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.audhd.aha.data.model.AppInfo
import org.audhd.aha.presentation.friction.MindfulDelayActivity

/**
 * Repository orchestrating launcher application discovery, indexing, and execution via [LauncherApps].
 *
 * Performance Invariants:
 * - Runs discovery strictly off the main thread on [Dispatchers.IO].
 * - Omits icon bitmap decodes entirely, retaining solely metadata string representations.
 */
class AppRepository(private val context: Context? = null) {

    private val launcherApps: LauncherApps? =
        context?.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val userManager: UserManager? =
        context?.getSystemService(Context.USER_SERVICE) as? UserManager

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    // Designated distraction/social apps requiring mindful friction
    private val defaultDistractionPackages = setOf(
        "com.instagram.android",
        "com.twitter.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.facebook.katana",
        "com.reddit.frontpage",
        "com.google.android.youtube",
        "com.snapchat.android"
    )

    /**
     * Queries the system for all launchable activities across all active user profiles.
     */
    suspend fun refreshApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val apps = mutableListOf<AppInfo>()
        val currentLauncher = launcherApps
        val users = userManager?.userProfiles ?: listOf(Process.myUserHandle())

        if (currentLauncher != null) {
            for (user in users) {
                val activities: List<LauncherActivityInfo> = try {
                    currentLauncher.getActivityList(null, user)
                } catch (e: Exception) {
                    emptyList()
                }

                for (activity in activities) {
                    if (activity.applicationInfo.packageName == context?.packageName) {
                        continue
                    }

                    val label = activity.label?.toString() ?: activity.applicationInfo.packageName
                    val pkgName = activity.applicationInfo.packageName
                    val activityName = activity.componentName.className

                    apps.add(
                        AppInfo(
                            label = label,
                            packageName = pkgName,
                            activityName = activityName,
                            userHandle = user,
                            isDistractionApp = defaultDistractionPackages.contains(pkgName)
                        )
                    )
                }
            }
        }

        // Fallback to PackageManager query if LauncherApps returned nothing (common on non-default launcher state)
        if (apps.isEmpty() && context != null) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = try {
                pm.queryIntentActivities(mainIntent, 0)
            } catch (e: Exception) {
                emptyList()
            }
            val defaultUser = Process.myUserHandle()
            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == context.packageName) continue
                val label = try {
                    resolveInfo.loadLabel(pm).toString()
                } catch (e: Exception) {
                    pkgName
                }
                val activityName = resolveInfo.activityInfo.name
                apps.add(
                    AppInfo(
                        label = label,
                        packageName = pkgName,
                        activityName = activityName,
                        userHandle = defaultUser,
                        isDistractionApp = defaultDistractionPackages.contains(pkgName)
                    )
                )
            }
        }

        // Sort alphabetically by app label
        val sortedApps = apps.sortedBy { it.label.lowercase() }
        _installedApps.value = sortedApps
        sortedApps
    }

    /**
     * Filters apps based on the user's input search query.
     */
    fun filterApps(query: String, sourceList: List<AppInfo> = _installedApps.value): List<AppInfo> {
        if (query.isBlank()) return sourceList
        val trimmed = query.trim().lowercase()
        return sourceList.filter { app ->
            app.searchIndex.contains(trimmed)
        }
    }

    /**
     * Launches the targeted application, routing through [MindfulDelayActivity] if flagged for mindful friction.
     */
    fun launchApp(app: AppInfo) {
        val ctx = context ?: return
        if (app.isDistractionApp) {
            val frictionIntent = Intent(ctx, MindfulDelayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(MindfulDelayActivity.EXTRA_TARGET_PACKAGE, app.packageName)
                putExtra(MindfulDelayActivity.EXTRA_TARGET_ACTIVITY, app.activityName)
                putExtra(MindfulDelayActivity.EXTRA_APP_LABEL, app.label)
            }
            ctx.startActivity(frictionIntent)
        } else {
            directLaunch(app)
        }
    }

    /**
     * Dispatches the launch directly using [LauncherApps].
     */
    fun directLaunch(app: AppInfo) {
        val ctx = context ?: return
        try {
            val user = app.userHandle ?: Process.myUserHandle()
            launcherApps?.startMainActivity(
                app.componentName,
                user,
                null,
                null
            )
        } catch (e: Exception) {
            // Fallback to standard launch intent
            val fallback = ctx.packageManager.getLaunchIntentForPackage(app.packageName)
            if (fallback != null) {
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(fallback)
            }
        }
    }
}
