// 文件路径: feature/bangumi/MyBangumiScreen.kt
package com.android.purebilibili.feature.bangumi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
// 🔥 已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.FollowBangumiItem

/**
 * 我的追番列表组件
 */
@Composable
fun MyBangumiContent(
    myFollowState: MyFollowState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onBangumiClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (myFollowState) {
        is MyFollowState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is MyFollowState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = myFollowState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                }
            }
        }
        is MyFollowState.Success -> {
            if (myFollowState.items.isEmpty()) {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "📺",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "还没有追番哦",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "去发现喜欢的番剧吧~",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                MyFollowGrid(
                    items = myFollowState.items,
                    hasMore = myFollowState.hasMore,
                    total = myFollowState.total,
                    onLoadMore = onLoadMore,
                    onItemClick = onBangumiClick,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun MyFollowGrid(
    items: List<FollowBangumiItem>,
    hasMore: Boolean,
    total: Int,
    onLoadMore: () -> Unit,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    
    // 加载更多检测
    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= layoutInfo.totalItemsCount - 4
        }.collect { shouldLoad ->
            if (shouldLoad && hasMore) {
                onLoadMore()
            }
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // 统计信息
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "共 $total 部追番",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 8.dp,
                end = 12.dp,
                bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()  // 🔥 添加导航栏内边距
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = items,
                key = { it.seasonId }
            ) { item ->
                MyFollowCard(
                    item = item,
                    onClick = { onItemClick(item.seasonId) }
                )
            }
            
            // 加载更多指示器
            if (hasMore) {
                item(span = { GridItemSpan(3) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyFollowCard(
    item: FollowBangumiItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = FormatUtils.fixImageUrl(item.cover),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )
            
            // 角标（会员专享等）
            if (item.badge.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // 更新状态标记
            if (item.newEp?.indexShow?.contains("更新") == true) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "更新",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // 底部信息
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                // 观看进度
                if (item.progress.isNotEmpty()) {
                    Text(
                        text = item.progress,
                        color = iOSYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 更新状态
                item.newEp?.indexShow?.let { indexShow ->
                    Text(
                        text = indexShow,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // 播放按钮 (悬浮)
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // 标题
        Text(
            text = item.title,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
