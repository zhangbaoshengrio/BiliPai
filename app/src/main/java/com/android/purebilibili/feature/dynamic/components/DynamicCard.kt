// 文件路径: feature/dynamic/components/DynamicCard.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.DynamicType

/**
 * 🔥 动态卡片V2 - 官方风格
 */
@Composable
fun DynamicCardV2(
    item: DynamicItem,
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit,
    onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> },
    gifImageLoader: ImageLoader
) {
    val author = item.modules.module_author
    val content = item.modules.module_dynamic
    val stat = item.modules.module_stat
    val type = DynamicType.fromApiValue(item.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // 🔥 用户头部（头像 + 名称 + 时间 + 更多）
        if (author != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(author.face.let { if (it.startsWith("http://")) it.replace("http://", "https://") else it })
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = author.mid > 0) { onUserClick(author.mid) },
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        author.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (author.vip?.status == 1) BiliPink else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        author.pub_time,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                }
                
                // 更多按钮
                IconButton(onClick = { /* TODO: 更多菜单 */ }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 🔥 动态内容文字（支持@高亮）
        content?.desc?.let { desc ->
            if (desc.text.isNotEmpty()) {
                RichTextContent(
                    desc = desc,
                    onUserClick = onUserClick
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        // 🔥 视频类型动态 - 大图预览
        content?.major?.archive?.let { archive ->
            VideoCardLarge(
                archive = archive,
                onClick = { onVideoClick(archive.bvid) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 🔥 图片类型动态（支持GIF + 点击预览）
        content?.major?.draw?.let { draw ->
            var selectedImageIndex by remember { mutableIntStateOf(-1) }
            
            DrawGridV2(
                items = draw.items,
                gifImageLoader = gifImageLoader,
                onImageClick = { index -> selectedImageIndex = index }
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // 全屏图片预览
            if (selectedImageIndex >= 0) {
                ImagePreviewDialog(
                    images = draw.items.map { it.src },
                    initialIndex = selectedImageIndex,
                    onDismiss = { selectedImageIndex = -1 }
                )
            }
        }
        
        // 🔥 直播推荐动态
        content?.major?.live_rcmd?.let { liveRcmd ->
            LiveCard(
                liveRcmd = liveRcmd,
                onLiveClick = onLiveClick
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 🔥 转发动态 - 嵌套显示原始内容
        if (type == DynamicType.FORWARD && item.orig != null) {
            ForwardedContent(
                orig = item.orig,
                onVideoClick = onVideoClick,
                onUserClick = onUserClick,
                gifImageLoader = gifImageLoader
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 🔥 交互按钮（转发 评论 点赞）
        if (stat != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = Icons.Default.Repeat,
                    count = stat.forward.count,
                    label = "转发"
                )
                ActionButton(
                    icon = Icons.Default.ChatBubbleOutline,
                    count = stat.comment.count,
                    label = "评论"
                )
                ActionButton(
                    icon = Icons.Default.FavoriteBorder,
                    count = stat.like.count,
                    label = "点赞",
                    activeColor = BiliPink
                )
            }
        }
    }
}

/**
 * 🔥 富文本内容（支持@提及高亮）
 */
@Composable
fun RichTextContent(
    desc: DynamicDesc,
    onUserClick: (Long) -> Unit
) {
    // 简化版：直接渲染文本，@提及用蓝色
    val text = buildAnnotatedString {
        val rawText = desc.text
        var lastEnd = 0
        
        // 查找 @xxx 模式
        val atPattern = Regex("@[^@\\s]+")
        atPattern.findAll(rawText).forEach { match ->
            // 普通文本
            if (match.range.first > lastEnd) {
                append(rawText.substring(lastEnd, match.range.first))
            }
            // @提及
            withStyle(SpanStyle(color = iOSBlue, fontWeight = FontWeight.Medium)) {
                append(match.value)
            }
            lastEnd = match.range.last + 1
        }
        // 剩余文本
        if (lastEnd < rawText.length) {
            append(rawText.substring(lastEnd))
        }
    }
    
    Text(
        text = text,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}
