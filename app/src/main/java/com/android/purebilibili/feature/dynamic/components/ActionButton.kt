// 文件路径: feature/dynamic/components/ActionButton.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.theme.iOSBlue

/**
 * 🍎 iOS 风格操作按钮 - 现代化胶囊设计
 */
@Composable
fun ActionButton(
    icon: ImageVector,
    count: Int,
    label: String,
    activeColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
) {
    val isLike = label == "点赞"
    val isForward = label == "转发"
    val isComment = label == "评论"
    
    // 🍎 iOS 风格按压动画
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "actionButtonScale"
    )
    
    // 🍎 iOS 风格颜色
    val buttonColor = when {
        isLike -> BiliPink
        isForward -> iOSBlue
        isComment -> MaterialTheme.colorScheme.primary
        else -> activeColor
    }
    
    // 🍎 优雅的图标
    val buttonIcon = when {
        isLike -> Icons.Outlined.FavoriteBorder
        isForward -> Icons.Outlined.Repeat
        isComment -> Icons.Outlined.ChatBubble
        else -> icon
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(
                color = buttonColor.copy(alpha = 0.08f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* TODO: 添加点击事件 */ }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // 🍎 使用 SF Symbols 风格图标
        Icon(
            imageVector = buttonIcon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = buttonColor
        )
        
        if (count > 0) {
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = when {
                    count >= 10000 -> "${count / 10000}万"
                    count >= 1000 -> String.format("%.1fk", count / 1000f)
                    else -> count.toString()
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = buttonColor,
                letterSpacing = (-0.3).sp  // 🍎 iOS 紧凑字距
            )
        }
    }
}
