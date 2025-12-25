// 文件路径: feature/settings/BottomBarSettingsScreen.kt
package com.android.purebilibili.feature.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.BottomBarColors  // 🔥 统一底栏颜色配置
import com.android.purebilibili.core.theme.BottomBarColorPalette  // 🔥 调色板
import com.android.purebilibili.core.theme.BottomBarColorNames  // 🔥 颜色名称
import kotlinx.coroutines.launch

/**
 * 🔥 底栏项目配置
 */
data class BottomBarTabConfig(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isDefault: Boolean = true  // 是否为默认项（默认项不可删除）
)

/**
 * 所有可用的底栏项目
 */
val allBottomBarTabs = listOf(
    BottomBarTabConfig("HOME", "首页", Icons.Filled.Home, isDefault = true),
    BottomBarTabConfig("DYNAMIC", "动态", Icons.Outlined.Subscriptions, isDefault = true),
    BottomBarTabConfig("HISTORY", "历史", Icons.Outlined.History, isDefault = true),
    BottomBarTabConfig("PROFILE", "我的", Icons.Outlined.AccountCircle, isDefault = true),
    BottomBarTabConfig("FAVORITE", "收藏", Icons.Outlined.FavoriteBorder, isDefault = false),
    BottomBarTabConfig("LIVE", "直播", Icons.Outlined.PlayArrow, isDefault = false),
    BottomBarTabConfig("WATCHLATER", "稍后看", Icons.Outlined.AccessTime, isDefault = false)
)

/**
 * 🔥 底栏管理设置页面
 * 支持拖拽排序和显示/隐藏配置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBarSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 读取当前配置
    val order by SettingsManager.getBottomBarOrder(context).collectAsState(initial = listOf("HOME", "DYNAMIC", "HISTORY", "PROFILE"))
    val visibleTabs by SettingsManager.getBottomBarVisibleTabs(context).collectAsState(initial = setOf("HOME", "DYNAMIC", "HISTORY", "PROFILE"))
    
    // 可编辑的本地状态
    var localOrder by remember(order) { mutableStateOf(order) }
    var localVisibleTabs by remember(visibleTabs) { mutableStateOf(visibleTabs) }
    
    // 🔥🔥 [新增] 读取项目颜色配置
    val itemColors by SettingsManager.getBottomBarItemColors(context).collectAsState(initial = emptyMap())
    
    // 保存配置
    fun saveConfig() {
        scope.launch {
            SettingsManager.setBottomBarOrder(context, localOrder)
            SettingsManager.setBottomBarVisibleTabs(context, localVisibleTabs)
        }
    }
    
    // 🔥🔥 [新增] 保存颜色配置
    fun saveItemColor(itemId: String, colorIndex: Int) {
        scope.launch {
            SettingsManager.setBottomBarItemColor(context, itemId, colorIndex)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("底栏管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 说明文字
            item {
                Text(
                    text = "选择要在底栏显示的项目，最少 2 个，最多 5 个。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 当前底栏预览
            item {
                SettingsSectionTitle("当前底栏")
            }
            
            item {
                BottomBarPreview(
                    tabs = localOrder.filter { it in localVisibleTabs }
                        .mapNotNull { id -> allBottomBarTabs.find { it.id == id } }
                )
            }
            
            // 可用项目列表
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionTitle("可用项目")
            }
            
            item {
                SettingsGroup {
                    allBottomBarTabs.forEachIndexed { index, tab ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                        }
                        BottomBarTabItem(
                            tab = tab,
                            isVisible = tab.id in localVisibleTabs,
                            colorIndex = itemColors[tab.id] ?: BottomBarColors.getDefaultColorIndex(tab.id),
                            canToggle = if (tab.id in localVisibleTabs) {
                                // 已显示的项目：至少保留 2 个可见
                                localVisibleTabs.size > 2
                            } else {
                                // 未显示的项目：最多显示 5 个
                                localVisibleTabs.size < 5
                            },
                            onToggle = { visible ->
                                localVisibleTabs = if (visible) {
                                    localVisibleTabs + tab.id
                                } else {
                                    localVisibleTabs - tab.id
                                }
                                // 如果是新增项目，加到顺序末尾
                                if (visible && tab.id !in localOrder) {
                                    localOrder = localOrder + tab.id
                                }
                                saveConfig()
                            },
                            onColorChange = { newColorIndex ->
                                saveItemColor(tab.id, newColorIndex)
                            }
                        )
                    }
                }
            }
            
            // 顺序调整说明
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 长按拖拽底栏图标可调整顺序（开发中）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // 重置按钮
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        localOrder = listOf("HOME", "DYNAMIC", "HISTORY", "PROFILE")
                        localVisibleTabs = setOf("HOME", "DYNAMIC", "HISTORY", "PROFILE")
                        saveConfig()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重置为默认")
                }
            }
        }
    }
}

/**
 * 底栏预览组件
 */
@Composable
private fun BottomBarPreview(tabs: List<BottomBarTabConfig>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 底栏项目单项
 */
@Composable
private fun BottomBarTabItem(
    tab: BottomBarTabConfig,
    isVisible: Boolean,
    colorIndex: Int,
    canToggle: Boolean,
    onToggle: (Boolean) -> Unit,
    onColorChange: (Int) -> Unit
) {
    // 🔥 获取项目当前颜色
    val itemColor = BottomBarColors.getColorByIndex(colorIndex)
    
    // 🔥 颜色选择弹窗状态
    var showColorPicker by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标 - 🔥 点击可更换颜色
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(itemColor.copy(alpha = 0.12f))
                .clickable { showColorPicker = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = itemColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        // 名称
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "点击图标更换颜色",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 开关
        Switch(
            checked = isVisible,
            onCheckedChange = { newValue -> if (canToggle) onToggle(newValue) },
            enabled = canToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    }
    
    // 🔥 颜色选择弹窗
    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("选择${tab.label}颜色") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BottomBarColorPalette.forEachIndexed { index, color ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onColorChange(index)
                                    showColorPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = BottomBarColorNames[index],
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (index == colorIndex) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}
