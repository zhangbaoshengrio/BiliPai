// 文件路径: feature/dynamic/components/ImagePreviewDialog.kt
package com.android.purebilibili.feature.dynamic.components

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🔥 图片预览对话框 - 支持左右切换和下载保存
 */
@Composable
fun ImagePreviewDialog(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    
    // 🔐 存储权限状态（Android 9 及以下需要）
    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }
    val storagePermission = com.android.purebilibili.core.util.rememberStoragePermissionState { granted ->
        if (granted && pendingSaveUrl != null) {
            // 权限授予后执行保存
            isSaving = true
            scope.launch {
                val success = saveImageToGallery(context, pendingSaveUrl!!)
                isSaving = false
                pendingSaveUrl = null
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (success) "图片已保存到相册" else "保存失败，请重试",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    // 规范化图片 URL
    val imageUrl = remember(images.getOrNull(currentIndex)) {
        val rawSrc = (images.getOrNull(currentIndex) ?: "").trim()
        when {
            rawSrc.startsWith("https://") -> rawSrc
            rawSrc.startsWith("http://") -> rawSrc.replace("http://", "https://")
            rawSrc.startsWith("//") -> "https:$rawSrc"
            rawSrc.isNotEmpty() -> "https://$rawSrc"
            else -> ""
        }
    }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            // 当前图片
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = {}),  // 阻止点击穿透
                contentScale = ContentScale.Fit
            )
            
            // 左右切换
            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 上一张
                    if (currentIndex > 0) {
                        FilledIconButton(
                            onClick = { currentIndex-- },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(0.3f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "上一张",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    
                    // 下一张
                    if (currentIndex < images.size - 1) {
                        FilledIconButton(
                            onClick = { currentIndex++ },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(0.3f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "下一张",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
                
                // 页码指示器
                Text(
                    "${currentIndex + 1} / ${images.size}",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            // 顶部按钮栏（关闭 + 下载）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 关闭按钮
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
                
                // 🔥 下载按钮
                IconButton(
                    onClick = {
                        if (!isSaving && imageUrl.isNotEmpty()) {
                            // 🔐 检查权限（Android 10+ 自动授权）
                            if (storagePermission.isGranted) {
                                isSaving = true
                                scope.launch {
                                    val success = saveImageToGallery(context, imageUrl)
                                    isSaving = false
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            if (success) "图片已保存到相册" else "保存失败，请重试",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                // 保存待执行的 URL，请求权限
                                pendingSaveUrl = imageUrl
                                storagePermission.request()
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "保存图片",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🔥 保存图片到相册
 */
suspend fun saveImageToGallery(context: android.content.Context, imageUrl: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // 使用 Coil 下载图片
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .addHeader("Referer", "https://www.bilibili.com/")
                .build()
            
            val result = imageLoader.execute(request)
            if (result !is SuccessResult) {
                Log.e("ImagePreview", "Failed to download image: $imageUrl")
                return@withContext false
            }
            
            val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bitmap == null) {
                Log.e("ImagePreview", "Failed to convert drawable to bitmap")
                return@withContext false
            }
            
            // 生成文件名
            val fileName = "BiliPai_${System.currentTimeMillis()}.jpg"
            
            // 使用 MediaStore 保存图片
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BiliPai")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext false
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
            
            // 标记保存完成
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            
            Log.d("ImagePreview", "Image saved successfully: $fileName")
            true
        } catch (e: Exception) {
            Log.e("ImagePreview", "Error saving image", e)
            false
        }
    }
}
