package com.android.purebilibili.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class VideoDetailResponse(
    // 👇 之前报错是因为缺了下面这行
    val code: Int = 0,
    val message: String = "",
    // 👆 补上就好了
    val data: ViewInfo? = null
)

/**
 * 🔥 视频尺寸信息
 * 用于判断横竖屏
 */
@Serializable
data class Dimension(
    val width: Int = 0,
    val height: Int = 0,
    val rotate: Int = 0
) {
    /** 是否为竖屏视频 (高度 > 宽度) */
    val isVertical: Boolean get() = height > width
}

@Serializable
data class ViewInfo(
    val bvid: String = "",
    val aid: Long = 0,
    val cid: Long = 0,
    val title: String = "",
    val desc: String = "",
    val pic: String = "",
    val pubdate: Long = 0,  // 🔥 发布时间戳 (秒)
    val tname: String = "", // 🔥 分区名称
    val owner: Owner = Owner(),
    val stat: Stat = Stat(),
    val pages: List<Page> = emptyList(),
    val dimension: Dimension? = null  // 🔥 视频尺寸信息
)

@Serializable
data class Page(
    val cid: Long = 0,
    val page: Int = 0,
    val from: String = "",
    val part: String = ""
)