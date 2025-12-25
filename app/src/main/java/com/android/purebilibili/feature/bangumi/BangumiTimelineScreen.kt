// 文件路径: feature/bangumi/BangumiTimelineScreen.kt
package com.android.purebilibili.feature.bangumi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
// 🔥 已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.TimelineDay
import com.android.purebilibili.data.model.response.TimelineEpisode

/**
 * 番剧时间表组件
 */
@Composable
fun BangumiTimelineContent(
    timelineState: TimelineState,
    onRetry: () -> Unit,
    onBangumiClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (timelineState) {
        is TimelineState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is TimelineState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timelineState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                }
            }
        }
        is TimelineState.Success -> {
            TimelineView(
                days = timelineState.days,
                onBangumiClick = onBangumiClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun TimelineView(
    days: List<TimelineDay>,
    onBangumiClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 找到今天的索引
    val todayIndex = days.indexOfFirst { it.isToday == 1 }.coerceAtLeast(0)
    var selectedDayIndex by remember { mutableIntStateOf(todayIndex) }
    
    val dayListState = rememberLazyListState()
    
    // 自动滚动到今天
    LaunchedEffect(todayIndex) {
        dayListState.animateScrollToItem(maxOf(0, todayIndex - 2))
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // 日期选择器
        LazyRow(
            state = dayListState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days.size) { index ->
                val day = days[index]
                DayChip(
                    day = day,
                    isSelected = index == selectedDayIndex,
                    onClick = { selectedDayIndex = index }
                )
            }
        }
        
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        
        // 当天番剧列表
        val selectedDay = days.getOrNull(selectedDayIndex)
        if (selectedDay != null && !selectedDay.episodes.isNullOrEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = selectedDay.episodes,
                    key = { it.episodeId }
                ) { episode ->
                    TimelineEpisodeCard(
                        episode = episode,
                        onClick = { onBangumiClick(episode.seasonId) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "今日无更新",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayChip(
    day: TimelineDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isToday = day.isToday == 1
    val weekDays = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val weekDay = weekDays.getOrElse(day.dayOfWeek) { "" }
    
    // 解析日期获取月日
    val dateParts = day.date.split("-")
    val displayDate = if (dateParts.size >= 3) {
        "${dateParts[1].toIntOrNull() ?: 0}/${dateParts[2].toIntOrNull() ?: 0}"
    } else {
        day.date
    }
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (isToday) "今天" else weekDay,
                fontSize = 14.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = displayDate,
                fontSize = 11.sp,
                color = when {
                    isSelected -> Color.White.copy(alpha = 0.8f)
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun TimelineEpisodeCard(
    episode: TimelineEpisode,
    onClick: () -> Unit
) {
    val isFollowed = episode.follow == 1
    val isDelayed = episode.delay == 1
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面
            Box(
                modifier = Modifier
                    .size(80.dp, 60.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = FormatUtils.fixImageUrl(episode.cover.ifEmpty { episode.squareCover }),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // 已追番标记
                if (isFollowed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "追番",
                            fontSize = 9.sp,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 信息区域
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = episode.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 更新集数
                    Text(
                        text = episode.pubIndex,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 更新时间
                    Text(
                        text = episode.pubTime,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 延迟信息
                if (isDelayed && episode.delayReason.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ ${episode.delayReason}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
