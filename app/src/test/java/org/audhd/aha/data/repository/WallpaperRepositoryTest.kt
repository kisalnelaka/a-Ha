package org.audhd.aha.data.repository

import org.audhd.aha.data.model.ProceduralWallpaperType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperRepositoryTest {

    private val repository = WallpaperRepository(null)


    @Test
    fun getProceduralPresets_containsFourEssentialDarkModes() {
        val presets = repository.getProceduralPresets()
        assertEquals(4, presets.size)
        assertTrue(presets.any { it.proceduralType == ProceduralWallpaperType.PURE_AMOLED_BLACK })
        assertTrue(presets.any { it.proceduralType == ProceduralWallpaperType.TWILIGHT_GRADIENT })
        assertTrue(presets.any { it.proceduralType == ProceduralWallpaperType.OBSIDIAN_DITHER })
        assertTrue(presets.any { it.proceduralType == ProceduralWallpaperType.MONOCHROME_HORIZON })
    }

    @Test
    fun getProceduralPresets_allFlaggedAsProcedural() {
        val presets = repository.getProceduralPresets()
        assertTrue(presets.all { it.isProcedural })
        assertTrue(presets.all { it.fullUrl.isEmpty() })
    }
}
