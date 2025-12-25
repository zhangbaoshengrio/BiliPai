package com.android.purebilibili.feature.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI 状态
sealed class SpaceUiState {
    object Loading : SpaceUiState()
    data class Success(
        val userInfo: SpaceUserInfo,
        val relationStat: RelationStatData? = null,
        val upStat: UpStatData? = null,
        val videos: List<SpaceVideoItem> = emptyList(),
        val totalVideos: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMoreVideos: Boolean = true,
        // 🔥 视频分类
        val categories: List<SpaceVideoCategory> = emptyList(),
        val selectedTid: Int = 0  // 0 表示全部
    ) : SpaceUiState()
    data class Error(val message: String) : SpaceUiState()
}

class SpaceViewModel : ViewModel() {
    
    private val spaceApi = NetworkModule.spaceApi
    
    private val _uiState = MutableStateFlow<SpaceUiState>(SpaceUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    private var currentMid: Long = 0
    private var currentPage = 1
    private val pageSize = 30
    
    // 🔥 缓存 WBI keys 避免重复请求
    private var cachedImgKey: String = ""
    private var cachedSubKey: String = ""
    
    fun loadSpaceInfo(mid: Long) {
        if (mid <= 0) return
        currentMid = mid
        currentPage = 1
        
        viewModelScope.launch {
            _uiState.value = SpaceUiState.Loading
            
            try {
                // 🔥 首先获取 WBI keys（只获取一次）
                val keys = fetchWbiKeys()
                if (keys == null) {
                    _uiState.value = SpaceUiState.Error("获取签名失败，请重试")
                    return@launch
                }
                cachedImgKey = keys.first
                cachedSubKey = keys.second
                
                // 并行请求用户信息、关注数、播放量统计
                val infoDeferred = async { fetchSpaceInfo(mid, cachedImgKey, cachedSubKey) }
                val relationDeferred = async { fetchRelationStat(mid) }
                val upStatDeferred = async { fetchUpStat(mid) }
                val videosDeferred = async { fetchSpaceVideos(mid, 1, cachedImgKey, cachedSubKey) }
                
                val userInfo = infoDeferred.await()
                val relationStat = relationDeferred.await()
                val upStat = upStatDeferred.await()
                val videosResult = videosDeferred.await()
                
                if (userInfo != null) {
                    val videos = videosResult?.list?.vlist ?: emptyList()
                    
                    // 🔥 调试日志
                    com.android.purebilibili.core.util.Logger.d("SpaceVM", "📊 Videos loaded: ${videos.size}")
                    videos.take(3).forEach { v ->
                        com.android.purebilibili.core.util.Logger.d("SpaceVM", "📊 Video: typeid=${v.typeid}, typename='${v.typename}', title=${v.title.take(20)}")
                    }
                    
                    val categories = extractCategories(videos)
                    com.android.purebilibili.core.util.Logger.d("SpaceVM", "📊 Categories extracted: ${categories.size} - ${categories.map { it.name }}")
                    
                    _uiState.value = SpaceUiState.Success(
                        userInfo = userInfo,
                        relationStat = relationStat,
                        upStat = upStat,
                        videos = videos,
                        totalVideos = videosResult?.page?.count ?: 0,
                        hasMoreVideos = videos.size >= pageSize,
                        categories = categories
                    )
                } else {
                    _uiState.value = SpaceUiState.Error("获取用户信息失败")
                }
            } catch (e: Exception) {
                android.util.Log.e("SpaceVM", "Error loading space: ${e.message}", e)
                _uiState.value = SpaceUiState.Error(e.message ?: "加载失败")
            }
        }
    }
    
    fun loadMoreVideos() {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMoreVideos) return
        
        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            
            try {
                val nextPage = currentPage + 1
                val result = fetchSpaceVideos(currentMid, nextPage, cachedImgKey, cachedSubKey)
                
                if (result != null) {
                    currentPage = nextPage
                    val newVideos = current.videos + (result.list.vlist)
                    _uiState.value = current.copy(
                        videos = newVideos,
                        isLoadingMore = false,
                        hasMoreVideos = result.list.vlist.size >= pageSize
                    )
                } else {
                    _uiState.value = current.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
                _uiState.value = current.copy(isLoadingMore = false)
            }
        }
    }
    
    // 🔥 获取 WBI 签名 keys
    private suspend fun fetchWbiKeys(): Pair<String, String>? {
        return try {
            val navResp = NetworkModule.api.getNavInfo()
            val wbiImg = navResp.data?.wbi_img ?: return null
            val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
            val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")
            Pair(imgKey, subKey)
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchWbiKeys error: ${e.message}")
            null
        }
    }
    
    private suspend fun fetchSpaceInfo(mid: Long, imgKey: String, subKey: String): SpaceUserInfo? {
        return try {
            val params = WbiUtils.sign(mapOf("mid" to mid.toString()), imgKey, subKey)
            com.android.purebilibili.core.util.Logger.d("SpaceVM", "🔍 fetchSpaceInfo params: $params")
            val response = spaceApi.getSpaceInfo(params)
            com.android.purebilibili.core.util.Logger.d("SpaceVM", "📦 fetchSpaceInfo response: code=${response.code}, message=${response.message}")
            if (response.code == 0) response.data else {
                android.util.Log.e("SpaceVM", "❌ fetchSpaceInfo failed: code=${response.code}, message=${response.message}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchSpaceInfo error: ${e.message}", e)
            null
        }
    }
    
    private suspend fun fetchRelationStat(mid: Long): RelationStatData? {
        return try {
            val response = spaceApi.getRelationStat(mid)
            if (response.code == 0) response.data else null
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun fetchUpStat(mid: Long): UpStatData? {
        return try {
            val response = spaceApi.getUpStat(mid)
            if (response.code == 0) response.data else null
        } catch (e: Exception) {
            null
        }
    }
    
    // 🔥 支持 tid 参数的视频获取
    private suspend fun fetchSpaceVideos(mid: Long, page: Int, imgKey: String, subKey: String, tid: Int = 0): SpaceVideoData? {
        return try {
            val params = WbiUtils.sign(mutableMapOf(
                "mid" to mid.toString(),
                "pn" to page.toString(),
                "ps" to pageSize.toString(),
                "order" to "pubdate"  // 按发布时间排序
            ).apply {
                if (tid > 0) put("tid", tid.toString())  // 🔥 添加分类筛选
            }.toMap(), imgKey, subKey)
            val response = spaceApi.getSpaceVideos(params)
            if (response.code == 0) response.data else null
        } catch (e: Exception) {
            android.util.Log.e("SpaceVM", "fetchSpaceVideos error: ${e.message}")
            null
        }
    }
    
    // 🔥 分类选择
    private var currentTid = 0
    
    fun selectCategory(tid: Int) {
        val current = _uiState.value as? SpaceUiState.Success ?: return
        if (current.selectedTid == tid) return  // 避免重复选择
        
        currentTid = tid
        currentPage = 1
        
        viewModelScope.launch {
            _uiState.value = current.copy(
                selectedTid = tid,
                videos = emptyList(),
                isLoadingMore = true
            )
            
            try {
                val result = fetchSpaceVideos(currentMid, 1, cachedImgKey, cachedSubKey, tid)
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                
                if (result != null) {
                    _uiState.value = currentState.copy(
                        videos = result.list.vlist,
                        totalVideos = result.page.count,
                        hasMoreVideos = result.list.vlist.size >= pageSize,
                        isLoadingMore = false
                    )
                } else {
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
                val currentState = _uiState.value as? SpaceUiState.Success ?: return@launch
                _uiState.value = currentState.copy(isLoadingMore = false)
            }
        }
    }
    
    // 🔥 解析分类信息 - 从视频列表中统计分类
    private fun extractCategories(videos: List<SpaceVideoItem>): List<SpaceVideoCategory> {
        // 即使 typename 为空，也使用 typeid 创建分类
        return videos
            .filter { it.typeid > 0 }
            .groupBy { it.typeid }
            .map { (tid, list) ->
                // 优先使用 typename，若为空则使用 typeid 作为名称
                val name = list.firstOrNull { it.typename.isNotEmpty() }?.typename 
                    ?: "分区$tid"
                SpaceVideoCategory(
                    tid = tid,
                    name = name,
                    count = list.size
                )
            }
            .sortedByDescending { it.count }
    }
}
