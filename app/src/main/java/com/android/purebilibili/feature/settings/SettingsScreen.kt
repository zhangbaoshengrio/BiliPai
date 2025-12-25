// 文件路径: feature/settings/SettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.viewmodel.compose.viewModel
// 🔥 已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.ui.AppIcons
import kotlinx.coroutines.launch
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoSlider
import io.github.alexzhirkevich.cupertino.CupertinoSliderDefaults
import io.github.alexzhirkevich.cupertino.theme.CupertinoColors

const val GITHUB_URL = "https://github.com/jay3-yy/BiliPai/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    onOpenSourceLicensesClick: () -> Unit,
    onAppearanceClick: () -> Unit = {},    // 🔥 外观设置
    onPlaybackClick: () -> Unit = {},      // 🔥 播放设置
    onPermissionClick: () -> Unit = {},    // 🔐 权限管理
    onPluginsClick: () -> Unit = {}        // 🔌 插件中心
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val state by viewModel.state.collectAsState()
    
    var showCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
    }
    
    // 🔥🔥 [修复] 设置导航栏透明，确保底部手势栏沉浸式效果
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val originalNavBarColor = window?.navigationBarColor ?: android.graphics.Color.TRANSPARENT
        
        if (window != null) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        
        onDispose {
            // 离开时恢复原始配置
            if (window != null) {
                window.navigationBarColor = originalNavBarColor
            }
        }
    }

    // 缓存清理弹窗
    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("清除缓存", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("确定要清除所有图片和视频缓存吗？", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCache()
                        Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
                        showCacheDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("确认清除") }
            },
            dismissButton = { TextButton(onClick = { showCacheDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        // 🔥🔥 [修复] 禁用 Scaffold 默认的 WindowInsets 消耗，避免底部白色填充
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            // 🔥🔥 [修复] 添加底部导航栏内边距，确保沉浸式效果
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            // ═══════════════════════════════════════════════════
            //  关注作者
            // ═══════════════════════════════════════════════════
            item { SettingsSectionTitle("关注作者") }
            item {
                SettingsGroup {
                // 🔥 使用 mono 图标 + iconTint，与其他设置项风格统一，自动支持深浅色
                SettingClickableItem(
                    iconPainter = androidx.compose.ui.res.painterResource(com.android.purebilibili.R.drawable.ic_telegram_mono),
                    title = "Telegram 频道",
                    value = "@BiliPai",
                    onClick = { uriHandler.openUri("https://t.me/BiliPai") },
                    iconTint = Color(0xFF0088CC)  // Telegram 品牌蓝
                )
                    Divider()
                    SettingClickableItem(
                        icon = AppIcons.Twitter,
                        title = "Twitter / X",
                        value = "@YangY_0x00",
                        onClick = { uriHandler.openUri("https://x.com/YangY_0x00") },
                        iconTint = Color(0xFF1DA1F2)
                    )
                }
            }
            
            // ═══════════════════════════════════════════════════
            // ⚙️ 常规设置
            // ═══════════════════════════════════════════════════
            item { SettingsSectionTitle("常规") }
            item {
                SettingsGroup {
                    SettingClickableItem(
                        icon = Icons.Outlined.Palette,
                        title = "外观设置",
                        value = "主题、图标、模糊效果",
                        onClick = onAppearanceClick,
                        iconTint = iOSPink
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.PlayCircleOutline,
                        title = "播放设置",
                        value = "解码、手势、后台播放",
                        onClick = onPlaybackClick,
                        iconTint = iOSGreen
                    )
                }
            }
            
            // ═══════════════════════════════════════════════════
            // 🔒 隐私与安全
            // ═══════════════════════════════════════════════════
            item { SettingsSectionTitle("隐私与安全") }
            item {
                val privacyModeEnabled by com.android.purebilibili.core.store.SettingsManager
                    .getPrivacyModeEnabled(context).collectAsState(initial = false)
                val scope = rememberCoroutineScope()
                
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.VisibilityOff,
                        title = "隐私无痕模式",
                        subtitle = "启用后不记录播放历史和搜索历史",
                        checked = privacyModeEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                com.android.purebilibili.core.store.SettingsManager
                                    .setPrivacyModeEnabled(context, enabled)
                            }
                        },
                        iconTint = iOSPurple
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.Security,
                        title = "权限管理",
                        value = "查看应用权限",
                        onClick = onPermissionClick,
                        iconTint = iOSTeal
                    )
                }
            }
            
            // ═══════════════════════════════════════════════════
            // 💾 数据与存储
            // ═══════════════════════════════════════════════════
            item { SettingsSectionTitle("数据与存储") }
            item {
                val scope = rememberCoroutineScope()
                val customDownloadPath by com.android.purebilibili.core.store.SettingsManager
                    .getDownloadPath(context).collectAsState(initial = null)
                val defaultPath = remember { 
                    com.android.purebilibili.core.store.SettingsManager.getDefaultDownloadPath(context) 
                }
                
                // SAF 目录选择器
                val directoryPicker = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    uri?.let { selectedUri ->
                        // 持久化权限
                        val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        context.contentResolver.takePersistableUriPermission(selectedUri, takeFlags)
                        
                        // 保存路径
                        scope.launch {
                            com.android.purebilibili.core.store.SettingsManager
                                .setDownloadPath(context, selectedUri.toString())
                        }
                        Toast.makeText(context, "下载路径已更新", Toast.LENGTH_SHORT).show()
                    }
                }
                
                var showPathDialog by remember { mutableStateOf(false) }
                
                // 路径选择对话框
                if (showPathDialog) {
                    AlertDialog(
                        onDismissRequest = { showPathDialog = false },
                        title = { Text("下载位置", color = MaterialTheme.colorScheme.onSurface) },
                        text = { 
                            Column {
                                Text(
                                    "默认位置（应用私有目录）：",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    defaultPath.substringAfterLast("Android/"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "💡 默认位置随应用卸载而删除，选择自定义位置可保留下载文件",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = iOSOrange
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showPathDialog = false
                                    directoryPicker.launch(null)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) { Text("选择自定义目录") }
                        },
                        dismissButton = { 
                            TextButton(
                                onClick = { 
                                    scope.launch {
                                        com.android.purebilibili.core.store.SettingsManager
                                            .setDownloadPath(context, null)
                                    }
                                    showPathDialog = false
                                    Toast.makeText(context, "已重置为默认路径", Toast.LENGTH_SHORT).show()
                                }
                            ) { 
                                Text("使用默认", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                            } 
                        },
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                }
                
                SettingsGroup {
                    // 下载位置
                    SettingClickableItem(
                        icon = Icons.Outlined.Folder,
                        title = "下载位置",
                        value = if (customDownloadPath != null) "自定义" else "默认",
                        onClick = { showPathDialog = true },
                        iconTint = iOSBlue
                    )
                    Divider()
                    // 清除缓存
                    SettingClickableItem(
                        icon = Icons.Outlined.DeleteOutline,
                        title = "清除缓存",
                        value = state.cacheSize,
                        onClick = { showCacheDialog = true },
                        iconTint = iOSPink
                    )
                }
            }
            
            // ═══════════════════════════════════════════════════
            // 🛠 开发者选项
            // ═══════════════════════════════════════════════════
            item { SettingsSectionTitle("开发者选项") }
            item {
                val crashTrackingEnabled by com.android.purebilibili.core.store.SettingsManager
                    .getCrashTrackingEnabled(context).collectAsState(initial = true)
                val analyticsEnabled by com.android.purebilibili.core.store.SettingsManager
                    .getAnalyticsEnabled(context).collectAsState(initial = true)
                val scope = rememberCoroutineScope()
                
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.BugReport,
                        title = "崩溃追踪",
                        subtitle = "帮助开发者发现和修复问题",
                        checked = crashTrackingEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                com.android.purebilibili.core.store.SettingsManager
                                    .setCrashTrackingEnabled(context, enabled)
                                com.android.purebilibili.core.util.CrashReporter.setEnabled(enabled)
                            }
                        },
                        iconTint = iOSTeal
                    )
                    Divider()
                    SettingSwitchItem(
                        icon = Icons.Outlined.Analytics,
                        title = "使用情况统计",
                        subtitle = "帮助改进应用体验，不收集个人信息",
                        checked = analyticsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                com.android.purebilibili.core.store.SettingsManager
                                    .setAnalyticsEnabled(context, enabled)
                                com.android.purebilibili.core.util.AnalyticsHelper.setEnabled(enabled)
                            }
                        },
                        iconTint = iOSBlue
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.Extension,
                        title = "插件中心",
                        value = "${com.android.purebilibili.core.plugin.PluginManager.getEnabledCount()} 个已启用",
                        onClick = onPluginsClick,
                        iconTint = iOSPurple
                    )
                    Divider()
                    // 📋 导出日志
                    SettingClickableItem(
                        icon = Icons.Outlined.Share,
                        title = "导出日志",
                        value = "用于反馈问题",
                        onClick = { 
                            com.android.purebilibili.core.util.LogCollector.exportAndShare(context)
                        },
                        iconTint = iOSTeal
                    )
                }
            }
            
            // ═══════════════════════════════════════════════════
            // ℹ️ 关于
            // ═══════════════════════════════════════════════════
            item { SettingsSectionTitle("关于") }
            item {
                SettingsGroup {
                    SettingClickableItem(
                        icon = Icons.Outlined.Description,
                        title = "开源许可证",
                        value = "License",
                        onClick = onOpenSourceLicensesClick,
                        iconTint = iOSOrange
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.Code,
                        title = "开源主页",
                        value = "GitHub",
                        onClick = { uriHandler.openUri(GITHUB_URL) },
                        iconTint = iOSPurple
                    )
                    Divider()
                    SettingClickableItem(
                        icon = Icons.Outlined.Info,
                        title = "版本",
                        value = "v${com.android.purebilibili.BuildConfig.VERSION_NAME}",
                        onClick = null,
                        iconTint = iOSTeal
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ... 底部组件封装保持不变 ...
@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),  // 🍎 iOS 风格大写
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,  // 🍎 更淡的颜色
        letterSpacing = 0.5.sp,  // 🍎 字符间距
        modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp)),  // 🍎 iOS 圆角
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,  // 🍎 iOS 不太使用阴影
        tonalElevation = 1.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingSwitchItem(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    // 🔥 新增：图标颜色
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            // 🔥 彩色圆形背景图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // 🍎 iOS 风格开关 - 🔥🔥 [修复] 使用主题色
        val primaryColor = MaterialTheme.colorScheme.primary
        CupertinoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = io.github.alexzhirkevich.cupertino.CupertinoSwitchDefaults.colors(
                thumbColor = Color.White,
                checkedTrackColor = primaryColor,
                uncheckedTrackColor = Color(0xFFE9E9EA)  // iOS 标准灰色
            )
        )
    }
}

@Composable
fun SettingClickableItem(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    // 🔥 新增：图标颜色
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null || iconPainter != null) {
            if (iconTint != Color.Unspecified) {
                // 🔥 彩色圆形背景图标
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    } else if (iconPainter != null) {
                        Icon(painter = iconPainter, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                // 🔥 使用图标原始颜色（无背景容器）
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                    } else if (iconPainter != null) {
                        Icon(painter = iconPainter, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (onClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.surfaceVariant))
}

fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)