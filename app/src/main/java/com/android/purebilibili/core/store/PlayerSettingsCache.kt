package com.android.purebilibili.core.store

import android.content.Context
import com.android.purebilibili.core.util.Logger

/**
 * 播放器设置缓存
 * 
 * 在 Application 启动时初始化，避免每次创建播放器时读取 SharedPreferences
 * 
 * 性能优化:
 * - 内存缓存硬件解码设置
 * - 避免重复 I/O 读取
 */
object PlayerSettingsCache {
    private const val TAG = "PlayerSettingsCache"
    private const val PREFS_NAME = "player_settings_cache"
    private const val KEY_HW_DECODE = "hw_decode_enabled"
    private const val KEY_SEEK_FAST = "seek_fast_enabled"
    private const val KEY_PLAYER_DIAGNOSTIC_LOGGING = "player_diagnostic_logging_enabled"
    private const val KEY_EXPAND_BUFFER = "expand_buffer_enabled"

    // 内存缓存
    @Volatile
    private var hwDecodeEnabled: Boolean? = null

    @Volatile
    private var seekFastEnabled: Boolean? = null

    @Volatile
    private var playerDiagnosticLoggingEnabled: Boolean? = null

    @Volatile
    private var expandBufferEnabled: Boolean? = null
    
    /**
     * 初始化缓存（在 Application.onCreate 中调用）
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        hwDecodeEnabled = prefs.getBoolean(KEY_HW_DECODE, true)
        seekFastEnabled = prefs.getBoolean(KEY_SEEK_FAST, true)
        playerDiagnosticLoggingEnabled = prefs.getBoolean(
            KEY_PLAYER_DIAGNOSTIC_LOGGING,
            DEFAULT_PLAYER_DIAGNOSTIC_LOGGING_ENABLED
        )
        expandBufferEnabled = prefs.getBoolean(KEY_EXPAND_BUFFER, false)
        Logger.d(
            TAG,
            "✅ 初始化完成: hwDecode=$hwDecodeEnabled, seekFast=$seekFastEnabled, " +
                "playerDiagnosticLogging=$playerDiagnosticLoggingEnabled, expandBuffer=$expandBufferEnabled"
        )
    }
    
    /**
     * 获取硬件解码设置（优先使用内存缓存）
     */
    fun isHwDecodeEnabled(context: Context): Boolean {
        return hwDecodeEnabled ?: run {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val value = prefs.getBoolean(KEY_HW_DECODE, true)
            hwDecodeEnabled = value
            value
        }
    }
    
    /**
     * 设置硬件解码开关
     */
    fun setHwDecodeEnabled(context: Context, enabled: Boolean) {
        hwDecodeEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HW_DECODE, enabled)
            .apply()
        Logger.d(TAG, "💾 硬件解码设置已更新: $enabled")
    }
    
    /**
     * 获取快速 Seek 设置
     */
    fun isSeekFastEnabled(context: Context): Boolean {
        return seekFastEnabled ?: run {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val value = prefs.getBoolean(KEY_SEEK_FAST, true)
            seekFastEnabled = value
            value
        }
    }
    
    /**
     * 设置快速 Seek 开关
     */
    fun setSeekFastEnabled(context: Context, enabled: Boolean) {
        seekFastEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEK_FAST, enabled)
            .apply()
        Logger.d(TAG, "💾 快速 Seek 设置已更新: $enabled")
    }

    /**
     * 获取播放器诊断日志开关
     */
    fun isPlayerDiagnosticLoggingEnabled(context: Context): Boolean {
        return playerDiagnosticLoggingEnabled ?: run {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val value = prefs.getBoolean(
                KEY_PLAYER_DIAGNOSTIC_LOGGING,
                DEFAULT_PLAYER_DIAGNOSTIC_LOGGING_ENABLED
            )
            playerDiagnosticLoggingEnabled = value
            value
        }
    }

    /**
     * 获取播放器诊断日志开关（仅读取内存缓存）
     */
    fun isPlayerDiagnosticLoggingEnabled(): Boolean {
        return playerDiagnosticLoggingEnabled ?: DEFAULT_PLAYER_DIAGNOSTIC_LOGGING_ENABLED
    }

    /**
     * 设置播放器诊断日志开关
     */
    fun setPlayerDiagnosticLoggingEnabled(context: Context, enabled: Boolean) {
        playerDiagnosticLoggingEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PLAYER_DIAGNOSTIC_LOGGING, enabled)
            .apply()
        Logger.d(TAG, "💾 播放器诊断日志设置已更新: $enabled")
    }
    
    /**
     * 获取扩展缓冲区设置（参考 PiliPlus expandBuffer，默认关闭）
     */
    fun isExpandBufferEnabled(context: Context): Boolean {
        return expandBufferEnabled ?: run {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val value = prefs.getBoolean(KEY_EXPAND_BUFFER, false)
            expandBufferEnabled = value
            value
        }
    }

    /**
     * 设置扩展缓冲区开关
     */
    fun setExpandBufferEnabled(context: Context, enabled: Boolean) {
        expandBufferEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EXPAND_BUFFER, enabled)
            .apply()
        Logger.d(TAG, "💾 扩展缓冲区设置已更新: $enabled")
    }

    /**
     * 强制刷新缓存（设置页面修改后调用）
     */
    fun refresh(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        hwDecodeEnabled = prefs.getBoolean(KEY_HW_DECODE, true)
        seekFastEnabled = prefs.getBoolean(KEY_SEEK_FAST, true)
        playerDiagnosticLoggingEnabled = prefs.getBoolean(
            KEY_PLAYER_DIAGNOSTIC_LOGGING,
            DEFAULT_PLAYER_DIAGNOSTIC_LOGGING_ENABLED
        )
        expandBufferEnabled = prefs.getBoolean(KEY_EXPAND_BUFFER, false)
        Logger.d(
            TAG,
            "🔄 缓存已刷新: hwDecode=$hwDecodeEnabled, seekFast=$seekFastEnabled, " +
                "playerDiagnosticLogging=$playerDiagnosticLoggingEnabled, expandBuffer=$expandBufferEnabled"
        )
    }
}
