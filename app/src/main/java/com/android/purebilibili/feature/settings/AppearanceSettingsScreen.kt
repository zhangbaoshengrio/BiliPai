// 文件路径: feature/settings/AppearanceSettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ArrowForward  // 🔥 底栏管理箭头
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.ui.blur.BlurIntensity
import kotlinx.coroutines.launch

/**
 * 🍎 外观设置二级页面
 * iOS 风格设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToBottomBarSettings: () -> Unit = {}  // 🔥🔥 [新增] 底栏设置导航
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    
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
    
    // 主题模式弹窗
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("外观模式", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (state.themeMode == mode),
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode.label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = { 
                TextButton(onClick = { showThemeDialog = false }) { 
                    Text("取消", color = MaterialTheme.colorScheme.primary) 
                } 
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观设置", fontWeight = FontWeight.SemiBold) },
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
            // 🍎 首页展示 - 抽屉式选择
            item { SettingsSectionTitle("首页展示") }
            item {
                SettingsGroup {
                    val displayMode = state.displayMode
                    var isExpanded by remember { mutableStateOf(false) }
                    
                    // 当前选中模式的名称
                    val currentModeName = DisplayMode.entries.find { it.value == displayMode }?.title ?: "双列网格"
                    
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 标题行 - 可点击展开/收起
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isExpanded = !isExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.GridView,
                                contentDescription = null,
                                tint = iOSBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "展示样式",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentModeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // 展开后的选项 - 带动画
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DisplayMode.entries.forEach { mode ->
                                    val isSelected = displayMode == mode.value
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .clickable {
                                                viewModel.setDisplayMode(mode.value)
                                                isExpanded = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                mode.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                        else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                mode.description,
                                                style = MaterialTheme.typography.bodySmall,
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
                    }
                }
            }

            // 🍎 深色模式
            item { SettingsSectionTitle("主题") }
            item {
                SettingsGroup {
                    SettingClickableItem(
                        icon = Icons.Outlined.DarkMode,
                        title = "深色模式",
                        value = state.themeMode.label,
                        onClick = { showThemeDialog = true },
                        iconTint = iOSBlue
                    )
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Divider()
                        SettingSwitchItem(
                            icon = Icons.Outlined.Palette,
                            title = "动态取色 (Material You)",
                            subtitle = "跟随系统壁纸变换应用主题色",
                            checked = state.dynamicColor,
                            onCheckedChange = { viewModel.toggleDynamicColor(it) },
                            iconTint = iOSPink
                        )
                        
                        // 🔥🔥 [新增] 动态取色预览
                        if (state.dynamicColor) {
                            DynamicColorPreview()
                        }
                    }
                    
                    Divider()
                    
                    // 主题色选择器
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.ColorLens,
                                contentDescription = null,
                                tint = iOSPink,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "主题色",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (state.dynamicColor) "已启用动态取色，此设置无效" 
                                           else "选择应用主色调",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            com.android.purebilibili.core.theme.ThemeColors.forEachIndexed { index, color ->
                                val isSelected = state.themeColorIndex == index && !state.dynamicColor
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (isSelected) Modifier.border(
                                                3.dp, 
                                                MaterialTheme.colorScheme.onSurface,
                                                CircleShape
                                            ) else Modifier
                                        )
                                        .clickable(enabled = !state.dynamicColor) { 
                                            viewModel.setThemeColorIndex(index) 
                                        }
                                        .graphicsLayer { 
                                            alpha = if (state.dynamicColor) 0.4f else 1f 
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 🍎 应用图标
            item { SettingsSectionTitle("图标") }
            item {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Apps,
                                contentDescription = null,
                                tint = iOSPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "应用图标",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "切换个性化启动图标",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        data class IconOption(val key: String, val name: String, val desc: String)
                        val iconOptions = listOf(
                            IconOption("3D", "3D立体", "默认"),
                            IconOption("Blue", "经典蓝", "原版"),
                            IconOption("Retro", "复古怀旧", "80年代"),
                            IconOption("Flat", "扁平现代", "Material"),
                            IconOption("Flat Material", "扁平材质", "Material You"),
                            IconOption("Neon", "霓虹", "夜间"),
                            IconOption("Telegram Blue", "纸飞机蓝", "Telegram"),
                            IconOption("Pink", "樱花粉", "可爱"),
                            IconOption("Purple", "香芋紫", "梦幻"),
                            IconOption("Green", "薄荷绿", "清新"),
                            IconOption("Dark", "暗夜蓝", "深色模式")
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(iconOptions.size) { index ->
                                val option = iconOptions[index]
                                val isSelected = state.appIcon == option.key
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { 
                                                if (!isSelected) {
                                                    Toast.makeText(context, "正在切换图标...", Toast.LENGTH_SHORT).show()
                                                    viewModel.setAppIcon(option.key)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val iconRes = when(option.key) {
                                            "3D" -> com.android.purebilibili.R.mipmap.ic_launcher_3d
                                            "Blue" -> com.android.purebilibili.R.mipmap.ic_launcher_blue
                                            "Retro" -> com.android.purebilibili.R.mipmap.ic_launcher_retro
                                            "Flat" -> com.android.purebilibili.R.mipmap.ic_launcher_flat
                                            "Flat Material" -> com.android.purebilibili.R.mipmap.ic_launcher_flat_material
                                            "Neon" -> com.android.purebilibili.R.mipmap.ic_launcher_neon
                                            "Telegram Blue" -> com.android.purebilibili.R.mipmap.ic_launcher_telegram_blue
                                            "Pink" -> com.android.purebilibili.R.mipmap.ic_launcher_telegram_pink
                                            "Purple" -> com.android.purebilibili.R.mipmap.ic_launcher_telegram_purple
                                            "Green" -> com.android.purebilibili.R.mipmap.ic_launcher_telegram_green
                                            "Dark" -> com.android.purebilibili.R.mipmap.ic_launcher_telegram_dark
                                            else -> com.android.purebilibili.R.mipmap.ic_launcher
                                        }
                                        AsyncImage(
                                            model = iconRes,
                                            contentDescription = option.name,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(Color.Black.copy(alpha = 0.3f))
                                            )
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        text = option.desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 🍎 界面效果
            item { SettingsSectionTitle("界面效果") }
            item {
                val scope = rememberCoroutineScope()
                val bottomBarVisibilityMode by com.android.purebilibili.core.store.SettingsManager
                    .getBottomBarVisibilityMode(context).collectAsState(
                        initial = com.android.purebilibili.core.store.SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE
                    )
                
                SettingsGroup {
                    // 🔥🔥 [导航入口] 底栏管理
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBottomBarSettings() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iOSBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Dashboard,
                                contentDescription = null,
                                tint = iOSBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "底栏管理",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "自定义底栏项目和顺序",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    
                    Divider()
                    
                    // ==================== 抽屉类选择器 ====================
                    
                    // 🔥 底栏显示模式选择（抽屉式）
                    var visibilityModeExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { visibilityModeExpanded = !visibilityModeExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = com.android.purebilibili.core.theme.iOSOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "底栏显示模式",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = bottomBarVisibilityMode.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (visibilityModeExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // 展开后的选项
                        androidx.compose.animation.AnimatedVisibility(
                            visible = visibilityModeExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                com.android.purebilibili.core.store.SettingsManager.BottomBarVisibilityMode.entries.forEach { mode ->
                                    val isSelected = mode == bottomBarVisibilityMode
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
                                                        .setBottomBarVisibilityMode(context, mode)
                                                }
                                                visibilityModeExpanded = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                mode.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                        else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                mode.description,
                                                style = MaterialTheme.typography.bodySmall,
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
                    }
                    
                    Divider()
                    
                    // 🔥 底栏标签样式（选择器）
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Dashboard,
                                contentDescription = null,
                                tint = iOSPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "底栏标签样式",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when(state.bottomBarLabelMode) {
                                        0 -> "图标 + 文字"
                                        2 -> "仅文字"
                                        else -> "仅图标"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 三种模式选择按钮
                            listOf(
                                Triple(0, "图标+文字", Icons.Outlined.Dashboard),
                                Triple(1, "仅图标", Icons.Outlined.Apps),
                                Triple(2, "仅文字", Icons.Outlined.TextFields)
                            ).forEach { (mode, label, icon) ->
                                val isSelected = state.bottomBarLabelMode == mode
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setBottomBarLabelMode(mode) }
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    
                    Divider()
                    
                    // ==================== 开关类设置 ====================
                    
                    // 🔥 悬浮底栏开关
                    SettingSwitchItem(
                        icon = Icons.Outlined.ViewStream,
                        title = "悬浮底栏",
                        subtitle = "关闭后底栏将沉浸式贴底显示",
                        checked = state.isBottomBarFloating,
                        onCheckedChange = { viewModel.toggleBottomBarFloating(it) },
                        iconTint = iOSTeal
                    )
                    
                    Divider()
                    
                    // 🔥 底栏磨砂效果开关
                    SettingSwitchItem(
                        icon = Icons.Outlined.BlurCircular,
                        title = "底栏磨砂效果",
                        subtitle = "底部导航栏的毛玻璃模糊",
                        checked = state.bottomBarBlurEnabled,
                        onCheckedChange = { viewModel.toggleBottomBarBlur(it) },
                        iconTint = iOSBlue
                    )
                    
                    // 🔥 模糊强度选择（仅在磨砂开启时显示）
                    if (state.bottomBarBlurEnabled) {
                        Divider()
                        BlurIntensitySelector(
                            selectedIntensity = state.blurIntensity,
                            onIntensityChange = { viewModel.setBlurIntensity(it) }
                        )
                    }
                    
                    Divider()
                    
                    // 🔥 卡片进场动画开关
                    SettingSwitchItem(
                        icon = Icons.Outlined.Animation,
                        title = "卡片进场动画",
                        subtitle = "首页视频卡片的入场动画效果",
                        checked = state.cardAnimationEnabled,
                        onCheckedChange = { viewModel.toggleCardAnimation(it) },
                        iconTint = iOSPink
                    )
                    
                    Divider()
                    
                    // 🔥 卡片过渡动画开关
                    SettingSwitchItem(
                        icon = Icons.Outlined.SwapHoriz,
                        title = "卡片过渡动画",
                        subtitle = "点击卡片时的共享元素过渡效果",
                        checked = state.cardTransitionEnabled,
                        onCheckedChange = { viewModel.toggleCardTransition(it) },
                        iconTint = iOSTeal
                    )
                }
            }
        }
    }
}
/**
 * 🔥 模糊强度选择器 (可展开/收起)
 */
@Composable
fun BlurIntensitySelector(
    selectedIntensity: BlurIntensity,
    onIntensityChange: (BlurIntensity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // 获取当前选中项的显示文本
    val currentTitle = when (selectedIntensity) {
        BlurIntensity.ULTRA_THIN -> "轻盈"
        BlurIntensity.THIN -> "标准"
        BlurIntensity.THICK -> "浓郁"
    }
    
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        // 标题行 - 可点击展开/收起
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.BlurOn,
                contentDescription = null,
                tint = iOSBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "模糊强度",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 展开/收起箭头
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // 展开后的选项 - 带动画
        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 8.dp)) {
                BlurIntensityOption(
                    title = "轻盈",
                    description = "通透感强，性能最佳",
                    isSelected = selectedIntensity == BlurIntensity.ULTRA_THIN,
                    onClick = { 
                        onIntensityChange(BlurIntensity.ULTRA_THIN)
                        isExpanded = false
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BlurIntensityOption(
                    title = "标准",
                    description = "平衡美观与性能（推荐）",
                    isSelected = selectedIntensity == BlurIntensity.THIN,
                    onClick = { 
                        onIntensityChange(BlurIntensity.THIN)
                        isExpanded = false
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BlurIntensityOption(
                    title = "浓郁",
                    description = "强烈磨砂质感",
                    isSelected = selectedIntensity == BlurIntensity.THICK,
                    onClick = { 
                        onIntensityChange(BlurIntensity.THICK)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun BlurIntensityOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 🔥🔥 动态取色预览组件
 * 显示从壁纸提取的 Material You 颜色
 */
@Composable
fun DynamicColorPreview() {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "当前取色预览",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Primary
            ColorPreviewItem(
                color = colorScheme.primary,
                label = "主色",
                modifier = Modifier.weight(1f)
            )
            // Secondary
            ColorPreviewItem(
                color = colorScheme.secondary,
                label = "辅色",
                modifier = Modifier.weight(1f)
            )
            // Tertiary
            ColorPreviewItem(
                color = colorScheme.tertiary,
                label = "第三色",
                modifier = Modifier.weight(1f)
            )
            // Primary Container
            ColorPreviewItem(
                color = colorScheme.primaryContainer,
                label = "容器",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ColorPreviewItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
