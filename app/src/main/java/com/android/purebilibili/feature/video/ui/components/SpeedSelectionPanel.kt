// 文件路径: feature/video/ui/components/SpeedSelectionPanel.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// 🔥 已改用 MaterialTheme.colorScheme.primary

/**
 * 播放速度选项
 */
object PlaybackSpeed {
    val OPTIONS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    
    fun formatSpeed(speed: Float): String {
        return if (speed == 1.0f) "倍速" else "${speed}x"
    }
    
    fun formatSpeedFull(speed: Float): String {
        return if (speed == 1.0f) "正常" else "${speed}x"
    }
}

/**
 * 播放速度选择菜单
 */
@Composable
fun SpeedSelectionMenu(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.85f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "播放速度",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 速度选项
            PlaybackSpeed.OPTIONS.forEach { speed ->
                val isSelected = speed == currentSpeed
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                    onClick = {
                        onSpeedSelected(speed)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = PlaybackSpeed.formatSpeedFull(speed),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

/**
 * 倍速按钮（用于底部控制栏）
 */
@Composable
fun SpeedButton(
    currentSpeed: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Text(
            text = PlaybackSpeed.formatSpeed(currentSpeed),
            color = if (currentSpeed != 1.0f) MaterialTheme.colorScheme.primary else Color.White,
            fontSize = 12.sp,
            fontWeight = if (currentSpeed != 1.0f) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
