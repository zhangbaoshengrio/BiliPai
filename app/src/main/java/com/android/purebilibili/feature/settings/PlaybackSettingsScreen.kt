// 文件路径: feature/settings/PlaybackSettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSSystemGray
import kotlinx.coroutines.launch

/**
 * 🍎 播放设置二级页面
 * iOS 风格设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    var isStatsEnabled by remember { mutableStateOf(prefs.getBoolean("show_stats", false)) }
    var showPipPermissionDialog by remember { mutableStateOf(false) }
    
    // 🔥🔥 [修复] 设置导航栏透明，确保底部手势栏沉浸式效果
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val originalNavBarColor = window?.navigationBarColor ?: android.graphics.Color.TRANSPARENT
        
        if (window != null) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        
        onDispose {
            if (window != null) {
                window.navigationBarColor = originalNavBarColor
            }
        }
    }
    
    // 检查画中画权限
    fun checkPipPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
        return false
    }
    
    // 跳转到系统设置
    fun gotoPipSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(
                    "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        }
    }
    
    // 权限弹窗
    if (showPipPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPipPermissionDialog = false },
            title = { Text("权限申请", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("检测到未开启「画中画」权限。请在设置中开启该权限，否则无法使用小窗播放。", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        gotoPipSettings()
                        showPipPermissionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showPipPermissionDialog = false }) {
                    Text("暂不开启", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("播放设置", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        // 🔥🔥 [修复] 禁用 Scaffold 默认的 WindowInsets 消耗，避免底部填充
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            // 🔥🔥 [修复] 添加底部导航栏内边距，确保沉浸式效果
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            // 🍎 解码设置
            item { SettingsSectionTitle("解码") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.Memory,
                        title = "启用硬件解码",
                        subtitle = "减少发热和耗电 (推荐开启)",
                        checked = state.hwDecode,
                        onCheckedChange = { viewModel.toggleHwDecode(it) },
                        iconTint = iOSGreen
                    )
                }
            }
            
            // 🍎 小窗播放
            item { SettingsSectionTitle("小窗播放") }
            item {
                val scope = rememberCoroutineScope()
                val miniPlayerMode by com.android.purebilibili.core.store.SettingsManager
                    .getMiniPlayerMode(context).collectAsState(
                        initial = com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.IN_APP_ONLY
                    )
                
                // 模式选项
                val modeOptions = com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.entries
                var isExpanded by remember { mutableStateOf(false) }
                
                SettingsGroup {
                    // 🍎 点击展开模式选择
                    SettingClickableItem(
                        icon = Icons.Outlined.PictureInPictureAlt,
                        title = "小窗模式",
                        value = miniPlayerMode.label,
                        onClick = { isExpanded = !isExpanded },
                        iconTint = iOSTeal
                    )
                    
                    // 🍎 展开的模式选择列表
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isExpanded,
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            modeOptions.forEach { mode ->
                                val isSelected = mode == miniPlayerMode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                        .clickable {
                                            scope.launch {
                                                com.android.purebilibili.core.store.SettingsManager
                                                    .setMiniPlayerMode(context, mode)
                                            }
                                            // 如果选择系统PiP，检查权限
                                            if (mode == com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.SYSTEM_PIP) {
                                                if (!checkPipPermission()) {
                                                    showPipPermissionDialog = true
                                                }
                                            }
                                            isExpanded = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            mode.label,
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            mode.description,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = "已选择",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 🔥 权限提示（仅当选择系统PiP且无权限时显示）
                    if (miniPlayerMode == com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.SYSTEM_PIP 
                        && !checkPipPermission()) {
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPipPermissionDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = iOSOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "画中画权限未开启",
                                    fontSize = 14.sp,
                                    color = iOSOrange
                                )
                                Text(
                                    "点击前往系统设置开启",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // 🍎 手势设置
            item { SettingsSectionTitle("手势控制") }
            item {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.TouchApp,
                                contentDescription = null,
                                tint = iOSOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "手势灵敏度",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "调整快进/音量/亮度手势响应速度",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${(state.gestureSensitivity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "较慢",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // 🍎 iOS 风格滑块
                            io.github.alexzhirkevich.cupertino.CupertinoSlider(
                                value = state.gestureSensitivity,
                                onValueChange = { viewModel.setGestureSensitivity(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 5,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            Text(
                                "较快",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 🍎 调试选项
            item { SettingsSectionTitle("调试") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.Info,
                        title = "详细统计信息",
                        subtitle = "显示 Codec、码率等 Geek 信息",
                        checked = isStatsEnabled,
                        onCheckedChange = {
                            isStatsEnabled = it
                            prefs.edit().putBoolean("show_stats", it).apply()
                        },
                        iconTint = iOSSystemGray
                    )
                }
            }
            
            // 🍎 交互设置
            item { SettingsSectionTitle("交互") }
            item {
                SettingsGroup {
                    SettingSwitchItem(
                        icon = Icons.Outlined.ThumbUp,
                        title = "双击点赞",
                        subtitle = "双击视频画面快捷点赞",
                        checked = state.doubleTapLike,
                        onCheckedChange = { viewModel.toggleDoubleTapLike(it) },
                        iconTint = com.android.purebilibili.core.theme.iOSPink
                    )
                }
            }
            
            // 🍎 网络与画质
            item { SettingsSectionTitle("网络与画质") }
            item {
                val scope = rememberCoroutineScope()
                val wifiQuality by com.android.purebilibili.core.store.SettingsManager
                    .getWifiQuality(context).collectAsState(initial = 80)
                val mobileQuality by com.android.purebilibili.core.store.SettingsManager
                    .getMobileQuality(context).collectAsState(initial = 64)
                
                // 画质选项列表
                val qualityOptions = listOf(
                    116 to "1080P60",
                    80 to "1080P",
                    64 to "720P",
                    32 to "480P",
                    16 to "360P"
                )
                
                fun getQualityLabel(id: Int) = qualityOptions.find { it.first == id }?.second ?: "720P"
                
                SettingsGroup {
                    // WiFi 画质选择
                    var wifiExpanded by remember { mutableStateOf(false) }
                    Column {
                        SettingClickableItem(
                            icon = Icons.Outlined.Wifi,
                            title = "WiFi 默认画质",
                            value = getQualityLabel(wifiQuality),
                            onClick = { wifiExpanded = !wifiExpanded },
                            iconTint = com.android.purebilibili.core.theme.iOSBlue
                        )
                        
                        // 🍎 展开动画
                        androidx.compose.animation.AnimatedVisibility(
                            visible = wifiExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                qualityOptions.forEach { (id, label) ->
                                    val isSelected = id == wifiQuality
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable {
                                                scope.launch { 
                                                    com.android.purebilibili.core.store.SettingsManager
                                                        .setWifiQuality(context, id)
                                                }
                                                wifiExpanded = false
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Divider()
                    
                    // 流量画质选择
                    var mobileExpanded by remember { mutableStateOf(false) }
                    Column {
                        SettingClickableItem(
                            icon = Icons.Outlined.SignalCellularAlt,
                            title = "流量 默认画质",
                            value = getQualityLabel(mobileQuality),
                            onClick = { mobileExpanded = !mobileExpanded },
                            iconTint = iOSOrange
                        )
                        
                        // 🍎 展开动画
                        androidx.compose.animation.AnimatedVisibility(
                            visible = mobileExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                qualityOptions.forEach { (id, label) ->
                                    val isSelected = id == mobileQuality
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable {
                                                scope.launch { 
                                                    com.android.purebilibili.core.store.SettingsManager
                                                        .setMobileQuality(context, id)
                                                }
                                                mobileExpanded = false
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
