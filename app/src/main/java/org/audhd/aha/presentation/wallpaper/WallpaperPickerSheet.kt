package org.audhd.aha.presentation.wallpaper

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.audhd.aha.data.model.WallpaperItem
import org.audhd.aha.data.repository.WallpaperRepository
import org.audhd.aha.data.repository.WallpaperTarget
import org.audhd.aha.domain.typography.FixationPointParser

@Composable
fun WallpaperPickerSheet(
    wallpaperRepository: WallpaperRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var wallpapers by remember { mutableStateOf<List<WallpaperItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }
    var pendingCustomUri by remember { mutableStateOf<Uri?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isApplying by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingCustomUri = uri
        }
    }

    suspend fun reloadWallpapers() {
        wallpapers = wallpaperRepository.getAvailableWallpapers()
    }

    LaunchedEffect(Unit) {
        reloadWallpapers()
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Wallpapers",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0E0E0),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Custom Photos • Offline Presets • Minimal",
                        fontSize = 11.sp,
                        color = Color(0xFF777777),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1C1C1C),
                        contentColor = Color(0xFFCCCCCC)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Close", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Custom Wallpaper Action Button
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    photoPicker.launch("image/*")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E281E),
                    contentColor = Color(0xFF88D888)
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, Color(0xFF2E4E2E), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "[ + CHOOSE FROM GALLERY / STORAGE ]",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            if (statusMessage != null) {
                Text(
                    text = statusMessage!!,
                    fontSize = 12.sp,
                    color = Color(0xFF88D888),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF888888),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(wallpapers, key = { it.id }) { item ->
                        WallpaperCard(
                            item = item,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                selectedWallpaper = item
                            },
                            onDelete = if (item.isCustom) {
                                {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    val deleted = wallpaperRepository.deleteCustomWallpaper(item)
                                    if (deleted) {
                                        statusMessage = "Custom wallpaper deleted."
                                        coroutineScope.launch { reloadWallpapers() }
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }

        // Custom Photo Target Dialog
        pendingCustomUri?.let { uri ->
            Dialog(onDismissRequest = { if (!isApplying) pendingCustomUri = null }) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF161616),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Import Custom Wallpaper",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEEEEEE),
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Downsampled & optimized for memory safety. Select destination:",
                            fontSize = 11.sp,
                            color = Color(0xFF888888),
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (isApplying) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(color = Color(0xFF888888), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Saving & applying wallpaper...",
                                    fontSize = 12.sp,
                                    color = Color(0xFFCCCCCC),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        isApplying = true
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        coroutineScope.launch {
                                            val ok = wallpaperRepository.importAndApplyCustomWallpaper(uri, WallpaperTarget.HOME)
                                            statusMessage = if (ok) "Custom wallpaper applied to Home." else "Failed to apply."
                                            reloadWallpapers()
                                            isApplying = false
                                            pendingCustomUri = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF242424),
                                        contentColor = Color(0xFFE0E0E0)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Apply to Home Screen", fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = {
                                        isApplying = true
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        coroutineScope.launch {
                                            val ok = wallpaperRepository.importAndApplyCustomWallpaper(uri, WallpaperTarget.LOCK)
                                            statusMessage = if (ok) "Custom wallpaper applied to Lock." else "Failed to apply."
                                            reloadWallpapers()
                                            isApplying = false
                                            pendingCustomUri = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF242424),
                                        contentColor = Color(0xFFE0E0E0)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Apply to Lock Screen", fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = {
                                        isApplying = true
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        coroutineScope.launch {
                                            val ok = wallpaperRepository.importAndApplyCustomWallpaper(uri, WallpaperTarget.BOTH)
                                            statusMessage = if (ok) "Custom wallpaper applied to Both." else "Failed to apply."
                                            reloadWallpapers()
                                            isApplying = false
                                            pendingCustomUri = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF333333),
                                        contentColor = Color(0xFFFFFFFF)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Apply to Both", fontFamily = FontFamily.Monospace)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { pendingCustomUri = null },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color(0xFF777777)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel", fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Apply Confirmation Dialog for presets / online wallpapers
        selectedWallpaper?.let { wallpaper ->
            Dialog(onDismissRequest = { if (!isApplying) selectedWallpaper = null }) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF161616),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = FixationPointParser.parse(wallpaper.title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEEEEEE),
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when {
                                wallpaper.isCustom -> "Imported Local Custom Photo"
                                wallpaper.isProcedural -> "Offline AMOLED Generated (0ms, 0 net)"
                                else -> "Curated Minimal Dark Stream"
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF888888),
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (isApplying) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF888888),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Setting wallpaper...",
                                    fontSize = 12.sp,
                                    color = Color(0xFFCCCCCC),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        isApplying = true
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        coroutineScope.launch {
                                            val ok = wallpaperRepository.applyWallpaper(wallpaper, WallpaperTarget.HOME)
                                            statusMessage = if (ok) "Wallpaper applied to Home Screen." else "Failed to apply."
                                            isApplying = false
                                            selectedWallpaper = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF242424),
                                        contentColor = Color(0xFFE0E0E0)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Apply to Home Screen", fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = {
                                        isApplying = true
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        coroutineScope.launch {
                                            val ok = wallpaperRepository.applyWallpaper(wallpaper, WallpaperTarget.LOCK)
                                            statusMessage = if (ok) "Wallpaper applied to Lock Screen." else "Failed to apply."
                                            isApplying = false
                                            selectedWallpaper = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF242424),
                                        contentColor = Color(0xFFE0E0E0)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Apply to Lock Screen", fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = {
                                        isApplying = true
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        coroutineScope.launch {
                                            val ok = wallpaperRepository.applyWallpaper(wallpaper, WallpaperTarget.BOTH)
                                            statusMessage = if (ok) "Wallpaper applied to Both Screens." else "Failed to apply."
                                            isApplying = false
                                            selectedWallpaper = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF333333),
                                        contentColor = Color(0xFFFFFFFF)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Apply to Both", fontFamily = FontFamily.Monospace)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { selectedWallpaper = null },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color(0xFF777777)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel", fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperCard(
    item: WallpaperItem,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF111111), RoundedCornerShape(6.dp))
            .border(
                1.dp,
                if (item.isCustom) Color(0xFF2E4E2E) else Color(0xFF222222),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = FixationPointParser.parse(item.title),
                fontSize = 14.sp,
                color = Color(0xFFDDDDDD),
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = when {
                    item.isCustom -> "Custom Saved Photo"
                    item.isProcedural -> "Offline Preset"
                    else -> "Wallhaven Minimal"
                },
                fontSize = 11.sp,
                color = when {
                    item.isCustom -> Color(0xFF88D888)
                    item.isProcedural -> Color(0xFF88AA88)
                    else -> Color(0xFF777777)
                },
                fontFamily = FontFamily.Monospace
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) {
                Text(
                    text = "[ Del ]",
                    fontSize = 11.sp,
                    color = Color(0xFFD88888),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clickable(onClick = onDelete)
                        .padding(end = 12.dp)
                )
            }

            Text(
                text = "Set →",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
