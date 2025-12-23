// 文件路径: core/plugin/json/JsonPluginManager.kt
package com.android.purebilibili.core.plugin.json

import android.content.Context
import com.android.purebilibili.core.plugin.DanmakuItem
import com.android.purebilibili.core.plugin.DanmakuStyle
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

private const val TAG = "JsonPluginManager"

/**
 * 🔌 JSON 规则插件管理器
 * 
 * 管理通过 URL 导入的 JSON 规则插件
 */
object JsonPluginManager {
    
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var appContext: Context
    
    /** 已加载的插件列表 */
    private val _plugins = MutableStateFlow<List<LoadedJsonPlugin>>(emptyList())
    val plugins: StateFlow<List<LoadedJsonPlugin>> = _plugins.asStateFlow()
    
    /** 🆕 过滤统计 (插件ID -> 过滤数量) */
    private val _filterStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val filterStats: StateFlow<Map<String, Int>> = _filterStats.asStateFlow()
    
    private var isInitialized = false
    
    /**
     * 初始化
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        isInitialized = true
        
        // 加载已保存的插件
        loadSavedPlugins()
        Logger.d(TAG, "🔌 JsonPluginManager initialized")
    }
    
    /**
     * 从 URL 导入插件
     */
    suspend fun importFromUrl(url: String): Result<JsonRulePlugin> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.d(TAG, "📥 下载插件: $url")
                
                // 🔥 使用带超时的 OkHttp 请求
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("下载失败: HTTP ${response.code} ${response.message}")
                    )
                }
                
                val content = response.body?.string()
                    ?: return@withContext Result.failure(Exception("服务器返回空内容"))
                
                Logger.d(TAG, "📄 下载内容长度: ${content.length}")
                
                val plugin = try {
                    json.decodeFromString<JsonRulePlugin>(content)
                } catch (e: Exception) {
                    Logger.e(TAG, "❌ JSON 解析失败", e)
                    return@withContext Result.failure(
                        Exception("JSON 解析失败: ${e.message?.take(100)}")
                    )
                }
                
                // 验证插件类型
                if (plugin.type !in listOf("feed", "danmaku")) {
                    return@withContext Result.failure(Exception("不支持的插件类型: ${plugin.type}"))
                }
                
                // 保存到本地
                savePlugin(plugin)
                
                // 添加到列表
                val loaded = LoadedJsonPlugin(plugin, enabled = true, sourceUrl = url)
                _plugins.value = _plugins.value.filter { it.plugin.id != plugin.id } + loaded
                
                Logger.d(TAG, "✅ 插件导入成功: ${plugin.name}")
                Result.success(plugin)
            } catch (e: java.net.SocketTimeoutException) {
                Logger.e(TAG, "❌ 连接超时", e)
                Result.failure(Exception("连接超时，请检查网络或 URL 是否正确"))
            } catch (e: java.net.UnknownHostException) {
                Logger.e(TAG, "❌ 无法解析主机", e)
                Result.failure(Exception("无法连接服务器，请检查 URL"))
            } catch (e: java.io.IOException) {
                Logger.e(TAG, "❌ 网络错误", e)
                Result.failure(Exception("网络错误: ${e.message}"))
            } catch (e: Exception) {
                Logger.e(TAG, "❌ 导入失败", e)
                Result.failure(Exception("导入失败: ${e.message?.take(100)}"))
            }
        }
    }
    
    /**
     * 删除插件
     */
    fun removePlugin(pluginId: String) {
        val file = File(getPluginDir(), "$pluginId.json")
        if (file.exists()) file.delete()
        
        _plugins.value = _plugins.value.filter { it.plugin.id != pluginId }
        Logger.d(TAG, "🗑️ 删除插件: $pluginId")
    }
    
    /**
     * 启用/禁用插件
     */
    fun setEnabled(pluginId: String, enabled: Boolean) {
        _plugins.value = _plugins.value.map { 
            if (it.plugin.id == pluginId) it.copy(enabled = enabled) else it
        }
        
        // 保存状态
        val prefs = appContext.getSharedPreferences("json_plugins", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("enabled_$pluginId", enabled).apply()
    }
    
    // ============ 过滤方法 ============
    
    /** 🆕 最近一次过滤掉的视频数量（用于 UI 提示） */
    private val _lastFilteredCount = MutableStateFlow(0)
    val lastFilteredCount: StateFlow<Int> = _lastFilteredCount.asStateFlow()
    
    /**
     * 过滤视频列表（带统计和计数）
     * @return 过滤后的视频列表
     */
    fun filterVideos(videos: List<VideoItem>): List<VideoItem> {
        val feedPlugins = _plugins.value.filter { it.enabled && it.plugin.type == "feed" }
        if (feedPlugins.isEmpty()) {
            _lastFilteredCount.value = 0
            return videos
        }
        
        var filteredCount = 0
        val result = videos.filter { video ->
            feedPlugins.all { loaded ->
                val show = RuleEngine.shouldShowVideo(video, loaded.plugin.rules)
                // 🆕 记录过滤统计
                if (!show) {
                    filteredCount++
                    val current = _filterStats.value.getOrDefault(loaded.plugin.id, 0)
                    _filterStats.value = _filterStats.value + (loaded.plugin.id to (current + 1))
                    Logger.d(TAG, "🚫 过滤视频: ${video.title.take(20)}... (插件: ${loaded.plugin.name})")
                }
                show
            }
        }
        
        // 🆕 更新最近过滤数量
        _lastFilteredCount.value = filteredCount
        if (filteredCount > 0) {
            Logger.d(TAG, "📊 本次过滤了 $filteredCount 个视频")
        }
        
        return result
    }
    
    /**
     * 🆕 更新插件规则
     */
    fun updatePlugin(plugin: JsonRulePlugin) {
        // 保存到本地
        savePlugin(plugin)
        
        // 更新列表（保留 enabled 状态）
        _plugins.value = _plugins.value.map { loaded ->
            if (loaded.plugin.id == plugin.id) {
                loaded.copy(plugin = plugin)
            } else loaded
        }
        
        // 重置该插件的统计
        _filterStats.value = _filterStats.value - plugin.id
        
        Logger.d(TAG, "✅ 插件已更新: ${plugin.name}")
    }
    
    /**
     * 🆕 重置统计
     */
    fun resetStats(pluginId: String? = null) {
        if (pluginId != null) {
            _filterStats.value = _filterStats.value - pluginId
        } else {
            _filterStats.value = emptyMap()
        }
    }
    
    /**
     * 过滤单个弹幕
     */
    fun shouldShowDanmaku(danmaku: DanmakuItem): Boolean {
        val danmakuPlugins = _plugins.value.filter { it.enabled && it.plugin.type == "danmaku" }
        return danmakuPlugins.all { loaded ->
            RuleEngine.shouldShowDanmaku(danmaku, loaded.plugin.rules)
        }
    }
    
    /**
     * 获取弹幕高亮样式
     */
    fun getDanmakuStyle(danmaku: DanmakuItem): DanmakuStyle? {
        val danmakuPlugins = _plugins.value.filter { it.enabled && it.plugin.type == "danmaku" }
        for (loaded in danmakuPlugins) {
            val style = RuleEngine.getDanmakuHighlightStyle(danmaku, loaded.plugin.rules)
            if (style != null) return style
        }
        return null
    }
    
    // ============ 私有方法 ============
    
    private fun getPluginDir(): File {
        val dir = File(appContext.filesDir, "json_plugins")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    private fun savePlugin(plugin: JsonRulePlugin) {
        val file = File(getPluginDir(), "${plugin.id}.json")
        file.writeText(json.encodeToString(JsonRulePlugin.serializer(), plugin))
    }
    
    private fun loadSavedPlugins() {
        val dir = getPluginDir()
        if (!dir.exists()) return
        
        val prefs = appContext.getSharedPreferences("json_plugins", Context.MODE_PRIVATE)
        
        val loaded = dir.listFiles()?.mapNotNull { file ->
            try {
                if (file.extension != "json") return@mapNotNull null
                val plugin = json.decodeFromString<JsonRulePlugin>(file.readText())
                val enabled = prefs.getBoolean("enabled_${plugin.id}", true)
                LoadedJsonPlugin(plugin, enabled, sourceUrl = null)
            } catch (e: Exception) {
                Logger.w(TAG, "⚠️ 加载插件失败: ${file.name}")
                null
            }
        } ?: emptyList()
        
        _plugins.value = loaded
        Logger.d(TAG, "📦 加载了 ${loaded.size} 个 JSON 插件")
    }
}

/**
 * 已加载的 JSON 插件
 */
data class LoadedJsonPlugin(
    val plugin: JsonRulePlugin,
    val enabled: Boolean,
    val sourceUrl: String?
)
