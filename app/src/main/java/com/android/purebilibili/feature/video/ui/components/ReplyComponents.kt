package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ReplyItem
import java.text.SimpleDateFormat
import java.util.*

// 🔥 优化后的颜色常量 (使用 MaterialTheme 替代硬编码)
// private val SubReplyBgColor = Color(0xFFF7F8FA)  // OLD
// private val TextSecondaryColor = Color(0xFF9499A0)  // OLD
// private val TextTertiaryColor = Color(0xFFB2B7BF)   // OLD

@Composable
fun ReplyHeader(count: Int) {
    Row(
        modifier = Modifier
        .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "评论",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = FormatUtils.formatStat(count.toLong()),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ReplyItemView(
    item: ReplyItem,
    emoteMap: Map<String, String> = emptyMap(),
    onClick: () -> Unit,
    onSubClick: (ReplyItem) -> Unit,
    onTimestampClick: ((Long) -> Unit)? = null  // 🔥 新增：时间戳点击回调
) {
    val localEmoteMap = remember(item.content.emote, emoteMap) {
        val mergedMap = emoteMap.toMutableMap()
        item.content.emote?.forEach { (key, value) -> mergedMap[key] = value.url }
        mergedMap
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 头像
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(item.member.avatar))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 🔥 用户名 + 等级 - 统一颜色风格
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.member.uname,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        // 🔥 VIP 用户使用粉色，普通用户使用次要色适配深色模式
                        color = if (item.member.vip?.vipStatus == 1) BiliPink
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // 🔥 优化后的等级标签
                    LevelTag(level = item.member.levelInfo.currentLevel)
                }
                
                Spacer(modifier = Modifier.height(6.dp))

                // 🔥🔥 正文 - 使用增强版 RichCommentText 支持时间戳点击
                RichCommentText(
                    text = item.content.message,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    emoteMap = localEmoteMap,
                    onTimestampClick = onTimestampClick
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 🔥 时间 + 点赞 + 回复 - 统一使用浅灰色
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(item.ctime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(20.dp))

                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "点赞",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    if (item.like > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = FormatUtils.formatStat(item.like.toLong()),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "回复",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onSubClick(item) }
                    )
                }

                // 🔥 楼中楼预览 - 使用更浅的背景色
                if (!item.replies.isNullOrEmpty() || item.rcount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // 🔥 适配深色
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSubClick(item) }
                            .padding(12.dp)
                    ) {
                        item.replies?.take(3)?.forEach { subReply ->
                            // 🔥🔥 [修复] 子评论也使用自己的表情映射
                            val subEmoteMap = remember(subReply.content.emote, emoteMap) {
                                val map = emoteMap.toMutableMap()
                                subReply.content.emote?.forEach { (key, value) -> map[key] = value.url }
                                map
                            }
                            
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                // 🔥 子评论用户名 - 使用统一的次要色
                                Text(
                                    text = subReply.member.uname,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = ": ",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // 🔥🔥 [修复] 子评论内容也使用 RichCommentText 显示表情
                                RichCommentText(
                                    text = subReply.content.message,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    emoteMap = subEmoteMap,
                                    maxLines = 2,
                                    onTimestampClick = onTimestampClick
                                )
                            }
                        }
                        if (item.rcount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "共${item.rcount}条回复 >",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 🔥 分割线 - 更细更浅
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 68.dp)  // 对齐头像右边
    )
}

/**
 * 🔥🔥 [新增] 富文本评论组件
 * 支持：表情渲染、时间戳点击跳转
 */
@Composable
fun RichCommentText(
    text: String,
    fontSize: TextUnit,
    color: Color = MaterialTheme.colorScheme.onSurface,
    emoteMap: Map<String, String>,
    maxLines: Int = Int.MAX_VALUE,
    onTimestampClick: ((Long) -> Unit)? = null
) {
    val timestampColor = MaterialTheme.colorScheme.primary
    
    // 🔥 时间戳正则: 支持 "1:23", "12:34", "1:23:45" 格式
    val timestampPattern = """(?<!\d)(\d{1,2}):(\d{2})(?::(\d{2}))?(?!\d)""".toRegex()
    
    val annotatedString = buildAnnotatedString {
        // 高亮 "回复 @某人 :"
        val replyPattern = "^回复 @(.*?) :".toRegex()
        val replyMatch = replyPattern.find(text)
        var startIndex = 0
        if (replyMatch != null) {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
                append(replyMatch.value)
            }
            startIndex = replyMatch.range.last + 1
        }

        val remainingText = text.substring(startIndex)
        val emotePattern = """\[(.*?)\]""".toRegex()
        
        // 🔥 收集所有匹配（表情 + 时间戳）并按位置排序
        data class MatchInfo(val range: IntRange, val type: String, val value: String, val seconds: Long = 0)
        val allMatches = mutableListOf<MatchInfo>()
        
        // 收集表情匹配
        emotePattern.findAll(remainingText).forEach { match ->
            allMatches.add(MatchInfo(match.range, "emote", match.value))
        }
        
        // 收集时间戳匹配
        timestampPattern.findAll(remainingText).forEach { match ->
            val hours = match.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            val minutes = match.groupValues[1].toIntOrNull() ?: 0
            val seconds = match.groupValues[2].toIntOrNull() ?: 0
            val totalSeconds = if (match.groupValues[3].isNotEmpty()) {
                // 格式: H:MM:SS
                match.groupValues[1].toInt() * 3600 + match.groupValues[2].toInt() * 60 + match.groupValues[3].toInt()
            } else {
                // 格式: MM:SS
                minutes * 60 + seconds
            }
            allMatches.add(MatchInfo(match.range, "timestamp", match.value, totalSeconds.toLong()))
        }
        
        // 按位置排序
        allMatches.sortBy { it.range.first }
        
        var lastIndex = 0
        allMatches.forEach { matchInfo ->
            // 添加匹配之前的普通文本
            if (lastIndex < matchInfo.range.first) {
                append(remainingText.substring(lastIndex, matchInfo.range.first))
            }
            
            when (matchInfo.type) {
                "emote" -> {
                    if (emoteMap.containsKey(matchInfo.value)) {
                        appendInlineContent(id = matchInfo.value, alternateText = matchInfo.value)
                    } else {
                        append(matchInfo.value)
                    }
                }
                "timestamp" -> {
                    // 🔥 时间戳使用特殊样式并添加点击注解
                    val annotationStart = length
                    pushStringAnnotation(tag = "TIMESTAMP", annotation = matchInfo.seconds.toString())
                    withStyle(SpanStyle(color = timestampColor, fontWeight = FontWeight.Medium)) {
                        append(matchInfo.value)
                    }
                    pop()
                }
            }
            
            lastIndex = matchInfo.range.last + 1
        }
        
        // 添加剩余文本
        if (lastIndex < remainingText.length) {
            append(remainingText.substring(lastIndex))
        }
    }

    val inlineContent = emoteMap.mapValues { (_, url) ->
        InlineTextContent(
            Placeholder(width = 1.4.em, height = 1.4.em, placeholderVerticalAlign = PlaceholderVerticalAlign.Center)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // 🔥 使用 Text + pointerInput 实现带表情的可点击文本
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        fontSize = fontSize,
        color = color,
        lineHeight = (fontSize.value * 1.5).sp,
        maxLines = maxLines,
        onTextLayout = { textLayoutResult = it },
        // 🔥🔥 [修复] 添加 padding 确保点击区域足够大
        modifier = Modifier.then(
            if (onTimestampClick != null) {
                Modifier.pointerInput(annotatedString) {
                    detectTapGestures { offset ->
                        textLayoutResult?.let { layoutResult ->
                            val position = layoutResult.getOffsetForPosition(offset)
                            // 🔥🔥 [修复] 扩大搜索范围，允许一定的点击容差
                            val searchStart = maxOf(0, position - 1)
                            val searchEnd = minOf(annotatedString.length, position + 1)
                            annotatedString.getStringAnnotations(
                                tag = "TIMESTAMP", 
                                start = searchStart, 
                                end = searchEnd
                            )
                                .firstOrNull()?.let { annotation ->
                                    val seconds = annotation.item.toLongOrNull() ?: 0L
                                    onTimestampClick(seconds * 1000)  // 转换为毫秒
                                }
                        }
                    }
                }
            } else Modifier
        )
    )
}

/**
 * 🔥 [兼容] 旧版 EmojiText (保持向后兼容)
 */
@Composable
fun EmojiText(
    text: String,
    fontSize: TextUnit,
    color: Color = MaterialTheme.colorScheme.onSurface,
    emoteMap: Map<String, String>
) {
    RichCommentText(
        text = text,
        fontSize = fontSize,
        color = color,
        emoteMap = emoteMap,
        onTimestampClick = null
    )
}

// 🔥🔥 [重构] 精简等级标签 - 纯文字显示，无背景边框
@Composable
fun LevelTag(level: Int) {
    // 🎨 B站官方配色方案 - 纯颜色文字
    val textColor = when {
        level >= 6 -> Color(0xFFFF6699)  // 粉色 (硬核用户)
        level >= 5 -> Color(0xFFFF9500)  // 橙色
        level >= 4 -> Color(0xFF22C3AA)  // 青绿色 (B站LV5标准色)
        level >= 3 -> Color(0xFF7BC549)  // 绿色
        level >= 2 -> Color(0xFF5EAADE)  // 蓝色
        else -> Color(0xFF969696)  // 灰色 (新用户)
    }
    
    Text(
        text = "LV$level",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
}

fun formatTime(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(date)
}