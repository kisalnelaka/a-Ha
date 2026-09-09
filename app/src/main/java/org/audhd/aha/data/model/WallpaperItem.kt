package org.audhd.aha.data.model

enum class ProceduralWallpaperType(val title: String) {
    PURE_AMOLED_BLACK("Pure AMOLED Black"),
    MATRIX_RAIN("Matrix Phosphor Stream"),
    MATRIX_GRID("Cyberpunk Tactical Grid"),
    OBSIDIAN_DITHER("Obsidian Subtle Dither"),
    TWILIGHT_GRADIENT("Circadian Twilight"),
    MONOCHROME_HORIZON("Minimal Horizon Line")
}

data class WallpaperItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String = "",
    val fullUrl: String = "",
    val isProcedural: Boolean = false,
    val proceduralType: ProceduralWallpaperType? = null,
    val isCustom: Boolean = false,
    val localFilePath: String = ""
)
