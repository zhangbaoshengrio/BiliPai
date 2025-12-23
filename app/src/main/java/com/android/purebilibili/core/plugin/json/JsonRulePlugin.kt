// 文件路径: core/plugin/json/JsonRulePlugin.kt
package com.android.purebilibili.core.plugin.json

import kotlinx.serialization.Serializable

/**
 * 🎯 JSON 规则插件数据模型
 */
@Serializable
data class JsonRulePlugin(
    val id: String,
    val name: String,
    val description: String = "",
    val version: String = "1.0.0",
    val author: String = "Unknown",
    val type: String,  // "feed" | "danmaku"
    val iconUrl: String? = null,  // 🆕 插件图标 URL
    val rules: List<Rule>
)

/**
 * 单条规则
 */
@Serializable
data class Rule(
    val field: String,        // 字段路径，如 "owner.mid", "title"
    val op: String,           // 操作符: eq, ne, lt, le, gt, ge, contains, startsWith, endsWith, regex, in
    val value: kotlinx.serialization.json.JsonElement,  // 比较值（支持多种类型）
    val action: String,       // 动作: hide, highlight
    val style: HighlightStyle? = null  // 仅 highlight 时使用
)

/**
 * 高亮样式
 */
@Serializable
data class HighlightStyle(
    val color: String? = null,     // 十六进制颜色 "#FFD700"
    val bold: Boolean = false,
    val scale: Float = 1.0f
)

/**
 * 规则操作符
 */
object RuleOperator {
    const val EQ = "eq"              // 等于
    const val NE = "ne"              // 不等于
    const val LT = "lt"              // 小于
    const val LE = "le"              // 小于等于
    const val GT = "gt"              // 大于
    const val GE = "ge"              // 大于等于
    const val CONTAINS = "contains"  // 包含
    const val STARTS_WITH = "startsWith"
    const val ENDS_WITH = "endsWith"
    const val REGEX = "regex"        // 正则匹配
    const val IN = "in"              // 在列表中
}

/**
 * 规则动作
 */
object RuleAction {
    const val HIDE = "hide"
    const val HIGHLIGHT = "highlight"
}
