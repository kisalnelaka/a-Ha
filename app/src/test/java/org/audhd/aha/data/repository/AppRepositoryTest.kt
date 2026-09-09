package org.audhd.aha.data.repository

import android.os.Process
import org.audhd.aha.data.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [AppRepository] filtering logic and search parsing.
 */
class AppRepositoryTest {

    private lateinit var repository: AppRepository
    private lateinit var sampleApps: List<AppInfo>

    @Before
    fun setUp() {
        repository = AppRepository(context = null)
        sampleApps = listOf(
            AppInfo(label = "Calculator", packageName = "com.android.calculator2", activityName = "Calculator", userHandle = null),
            AppInfo(label = "Camera", packageName = "com.android.camera2", activityName = "Camera", userHandle = null),
            AppInfo(label = "Clock", packageName = "com.android.deskclock", activityName = "DeskClock", userHandle = null),
            AppInfo(label = "Instagram", packageName = "com.instagram.android", activityName = "MainActivity", userHandle = null, isDistractionApp = true),
            AppInfo(label = "Settings", packageName = "com.android.settings", activityName = "Settings", userHandle = null)
        )
    }

    @Test
    fun `filterApps returns full list on blank query`() {
        val result = repository.filterApps("", sampleApps)
        assertEquals(5, result.size)

        val resultWhitespace = repository.filterApps("   ", sampleApps)
        assertEquals(5, resultWhitespace.size)
    }

    @Test
    fun `filterApps correctly matches substring case-insensitively`() {
        val result = repository.filterApps("cam", sampleApps)
        assertEquals(1, result.size)
        assertEquals("Camera", result[0].label)
    }

    @Test
    fun `filterApps matches package name substring`() {
        val result = repository.filterApps("deskclock", sampleApps)
        assertEquals(1, result.size)
        assertEquals("Clock", result[0].label)
    }

    @Test
    fun `filterApps returns empty list when no matches exist`() {
        val result = repository.filterApps("nonexistentappxyz", sampleApps)
        assertTrue(result.isEmpty())
    }
}
