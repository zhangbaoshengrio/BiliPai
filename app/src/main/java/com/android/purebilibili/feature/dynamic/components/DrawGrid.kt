// 文件路径: feature/dynamic/components/DrawGrid.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.android.purebilibili.data.model.response.DrawItem

/**
 * 🔥 图片九宫格V2（支持GIF + 点击预览）
 */
@Composable
fun DrawGridV2(
    items: List<DrawItem>,
    gifImageLoader: ImageLoader,
    onImageClick: (Int) -> Unit = {}  // 🔥 图片点击回调
) {
    if (items.isEmpty()) return
    
    val context = LocalContext.current
    val displayItems = items.take(9)
    val columns = when {
        displayItems.size == 1 -> 1
        displayItems.size <= 4 -> 2
        else -> 3
    }
    
    val singleImageRatio = if (displayItems.size == 1 && displayItems[0].width > 0 && displayItems[0].height > 0) {
        displayItems[0].width.toFloat() / displayItems[0].height.toFloat()
    } else {
        1f
    }
    
    var globalIndex = 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        displayItems.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { item ->
                    val currentIndex = globalIndex++
                    val imageUrl = remember(item.src) {
                        val rawSrc = item.src.trim()
                        when {
                            rawSrc.startsWith("https://") -> rawSrc
                            rawSrc.startsWith("http://") -> rawSrc.replace("http://", "https://")
                            rawSrc.startsWith("//") -> "https:$rawSrc"
                            rawSrc.isNotEmpty() -> "https://$rawSrc"
                            else -> ""
                        }
                    }
                    
                    val aspectRatio = if (displayItems.size == 1) singleImageRatio else 1f
                    val isGif = imageUrl.endsWith(".gif", ignoreCase = true)
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onImageClick(currentIndex) },  // 🔥 点击预览
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .addHeader("Referer", "https://www.bilibili.com/")
                                    .crossfade(!isGif)
                                    .build(),
                                imageLoader = if (isGif) gifImageLoader else ImageLoader(context),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.Gray.copy(0.5f)
                            )
                        }
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
