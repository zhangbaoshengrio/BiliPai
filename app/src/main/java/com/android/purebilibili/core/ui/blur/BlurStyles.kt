// 文件路径: core/ui/blur/BlurStyles.kt
package com.android.purebilibili.core.ui.blur

import androidx.compose.runtime.Composable
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * 🔥🔥 模糊强度枚举
 * 用户可选的三种模糊强度等级
 */
enum class BlurIntensity {
    ULTRA_THIN,  // 轻盈 - 通透感强
    THIN,        // 标准 - 平衡美观与性能（默认）
    THICK        // 浓郁 - 强烈磨砂质感
}

/**
 * 🎨 模糊样式管理
 * 
 * ⚠️ 注意：Haze 库命名与直觉相反！
 * - HazeMaterials.ultraThin() 实际效果最浓郁
 * - HazeMaterials.thick() 实际效果最轻盈
 */
object BlurStyles {
    
    @Composable
    fun getBlurStyle(intensity: BlurIntensity): HazeStyle {
        return when (intensity) {
            BlurIntensity.ULTRA_THIN -> HazeMaterials.thick()      // 轻盈：用 thick()
            BlurIntensity.THIN -> HazeMaterials.thin()             // 标准
            BlurIntensity.THICK -> HazeMaterials.ultraThin()       // 浓郁：用 ultraThin()
        }
    }
}
