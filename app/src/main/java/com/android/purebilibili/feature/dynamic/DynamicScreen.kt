// 文件路径: feature/dynamic/DynamicScreen.kt
package com.android.purebilibili.feature.dynamic

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.android.purebilibili.core.ui.BiliGradientButton
import com.android.purebilibili.core.ui.EmptyState
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.feature.dynamic.components.DynamicCardV2
import com.android.purebilibili.feature.dynamic.components.DynamicSidebar
import com.android.purebilibili.feature.dynamic.components.DynamicTopBarWithTabs

/**
 * 🔥 动态页面 - 官方风格重构版
 * 
 * 组件已拆分至 components/ 目录：
 * - DynamicTopBar.kt      顶栏 + Tabs
 * - DynamicSidebar.kt     侧边栏组件
 * - DynamicCard.kt        动态卡片 V2
 * - VideoCards.kt         视频卡片（大/小）
 * - LiveCard.kt           直播卡片
 * - DrawGrid.kt           图片九宫格
 * - ForwardedContent.kt   转发内容
 * - ActionButton.kt       操作按钮
 * - ImagePreviewDialog.kt 图片预览对话框
 * 
 * 数据模型位于 model/ 目录：
 * - LiveContentModels.kt  直播内容数据类
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicScreen(
    viewModel: DynamicViewModel = viewModel(),
    onVideoClick: (String) -> Unit,
    onUserClick: (Long) -> Unit = {},
    onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    onLoginClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}  // 🔥 返回视频首页
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val listState = rememberLazyListState()
    
    // 🔥 侧边栏状态
    val followedUsers by viewModel.followedUsers.collectAsState()
    val selectedUserId by viewModel.selectedUserId.collectAsState()
    val isSidebarExpanded by viewModel.isSidebarExpanded.collectAsState()
    
    // 🔥 Tab选择
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("全部", "视频")
    
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    val pullRefreshState = rememberPullToRefreshState()
    
    // 🔥 GIF图片加载器
    val context = LocalContext.current
    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }
    
    // 🔥 过滤动态（Tab + 用户选择）
    val filteredItems = remember(state.items, selectedTab, selectedUserId) {
        var items = state.items
        // Tab 过滤
        if (selectedTab == 1) {
            items = items.filter { it.type == "DYNAMIC_TYPE_AV" }
        }
        // 用户过滤
        selectedUserId?.let { uid ->
            items = items.filter { it.modules.module_author?.mid == uid }
        }
        // 🔥 [修复] 去重防止 LazyColumn key 冲突崩溃
        items.distinctBy { it.id_str }
    }
    
    // 加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 3 && !state.isLoading && state.hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 🔥 左侧边栏
            DynamicSidebar(
                users = followedUsers,
                selectedUserId = selectedUserId,
                isExpanded = isSidebarExpanded,
                onUserClick = { viewModel.selectUser(it) },
                onToggleExpand = { viewModel.toggleSidebar() },
                modifier = Modifier.padding(top = statusBarHeight)
            )
            
            // 🔥 右侧内容区
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullRefreshState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = statusBarHeight + 100.dp,  // 顶栏 + Tab 高度
                    bottom = 80.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                // 空状态
                if (filteredItems.isEmpty() && !state.isLoading && state.error == null) {
                    item {
                        EmptyState(
                            message = "暂无动态",
                            actionText = "登录后查看关注 UP主 的动态",
                            modifier = Modifier.height(300.dp)
                        )
                    }
                }
                
                // 动态卡片列表
                items(filteredItems, key = { "dynamic_${it.id_str}" }) { item ->
                    DynamicCardV2(
                        item = item,
                        onVideoClick = onVideoClick,
                        onUserClick = onUserClick,
                        onLiveClick = onLiveClick,
                        gifImageLoader = gifImageLoader
                    )
                    
                    // 分隔线
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
                
                // 加载中
                if (state.isLoading && state.items.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingAnimation(size = 40.dp)
                        }
                    }
                }
                
                // 没有更多
                if (!state.hasMore && filteredItems.isNotEmpty()) {
                    item {
                        Text(
                            "没有更多了",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            
            // 🔥 顶栏 + Tab
            DynamicTopBarWithTabs(
                selectedTab = selectedTab,
                tabs = tabs,
                onTabSelected = { selectedTab = it },
                onBackClick = onHomeClick,  // 🔥 返回视频首页
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            // 错误提示
            if (state.error != null && state.items.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (state.error?.contains("未登录") == true) {
                        BiliGradientButton(text = "去登录", onClick = onLoginClick)
                    } else {
                        BiliGradientButton(text = "重试", onClick = { viewModel.refresh() })
                    }
                }
            }
            }
        }  // End Row
    }
}
