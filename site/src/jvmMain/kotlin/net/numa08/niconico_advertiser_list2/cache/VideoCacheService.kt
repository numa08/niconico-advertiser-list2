package net.numa08.niconico_advertiser_list2.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import kotlinx.coroutines.runBlocking
import net.numa08.niconico_advertiser_list2.datasource.HttpClientFactory
import net.numa08.niconico_advertiser_list2.datasource.NicoadDataSource
import net.numa08.niconico_advertiser_list2.models.NicoadHistory
import net.numa08.niconico_advertiser_list2.models.VideoInfo
import net.numa08.niconico_advertiser_list2.util.RequestSemaphore
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * 動画情報とキャッシュメタデータ
 */
data class CachedVideoInfo(
    val videoInfo: VideoInfo,
    val cachedAt: Instant,
)

/**
 * 広告履歴とキャッシュメタデータ
 */
data class CachedNicoadHistory(
    val histories: List<NicoadHistory>,
    val cachedAt: Instant,
)

/**
 * Caffeine LoadingCacheを使った動画キャッシュサービス
 *
 * Caffeineのキャッシュライブラリを使用して、
 * 動画情報と広告履歴を効率的にキャッシュします。
 */
object VideoCacheService {
    private const val CACHE_TTL_HOURS = 1L
    private const val MAX_CACHE_SIZE = 1000L

    // DataSourceインスタンス
    private val dataSource = NicoadDataSource(HttpClientFactory.httpClient)

    /**
     * 動画情報のLoadingCache
     * キャッシュミス時は自動的にニコニコ動画APIから取得
     */
    val videoInfoCache: LoadingCache<String, CachedVideoInfo> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(CACHE_TTL_HOURS, TimeUnit.HOURS)
            .maximumSize(MAX_CACHE_SIZE)
            .recordStats()
            .build { videoId ->
                // キャッシュミス時に自動実行されるローダー
                // runBlockingでsuspend関数を呼び出す
                runBlocking {
                    val result =
                        RequestSemaphore.withLimit {
                            dataSource.getVideoInformation(videoId)
                        }

                    val response = result.getOrThrow()

                    CachedVideoInfo(
                        videoInfo =
                            VideoInfo(
                                videoId = response.videoId,
                                title = response.title,
                                thumbnail = response.thumbnail,
                                userId = response.userId,
                            ),
                        cachedAt = Instant.now(),
                    )
                }
            }

    /**
     * 広告履歴のLoadingCache
     * キャッシュミス時は自動的にニコニコ広告APIから取得
     */
    val nicoadHistoryCache: LoadingCache<String, CachedNicoadHistory> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(CACHE_TTL_HOURS, TimeUnit.HOURS)
            .maximumSize(MAX_CACHE_SIZE)
            .recordStats()
            .build { videoId ->
                runBlocking {
                    val result =
                        RequestSemaphore.withLimit {
                            dataSource.getNicoadHistories(videoId)
                        }

                    val responses = result.getOrThrow()
                    val histories =
                        responses.map { response ->
                            NicoadHistory(
                                advertiserName = response.advertiserName,
                                nicoadId = response.nicoadId,
                                adPoint = response.adPoint,
                                contribution = response.contribution,
                                startedAt = response.startedAt,
                                endedAt = response.endedAt,
                                userId = response.userId,
                                message = response.message,
                            )
                        }

                    CachedNicoadHistory(
                        histories = histories,
                        cachedAt = Instant.now(),
                    )
                }
            }

    /**
     * 動画情報を取得
     * キャッシュにあればキャッシュから、なければ自動的にロードして返す
     *
     * @return Result<CachedVideoInfo> 成功時はキャッシュされた動画情報、失敗時はエラー
     */
    fun getVideoInfo(videoId: String): Result<CachedVideoInfo> = runCatching { videoInfoCache.get(videoId) }

    /**
     * 広告履歴を取得
     * キャッシュにあればキャッシュから、なければ自動的にロードして返す
     *
     * @return Result<CachedNicoadHistory> 成功時はキャッシュされた広告履歴、失敗時はエラー
     */
    fun getNicoadHistory(videoId: String): Result<CachedNicoadHistory> = runCatching { nicoadHistoryCache.get(videoId) }

    /**
     * 動画情報を強制更新
     * キャッシュを削除して再取得
     *
     * @return Result<CachedVideoInfo> 成功時はキャッシュされた動画情報、失敗時はエラー
     */
    fun refreshVideoInfo(videoId: String): Result<CachedVideoInfo> =
        runCatching {
            videoInfoCache.invalidate(videoId)
            videoInfoCache.get(videoId)
        }

    /**
     * 広告履歴を強制更新
     * キャッシュを削除して再取得
     *
     * @return Result<CachedNicoadHistory> 成功時はキャッシュされた広告履歴、失敗時はエラー
     */
    fun refreshNicoadHistory(videoId: String): Result<CachedNicoadHistory> =
        runCatching {
            nicoadHistoryCache.invalidate(videoId)
            nicoadHistoryCache.get(videoId)
        }

    /**
     * 特定動画の全キャッシュを削除
     */
    fun invalidateAll(videoId: String) {
        videoInfoCache.invalidate(videoId)
        nicoadHistoryCache.invalidate(videoId)
    }

    /**
     * キャッシュ統計情報を取得
     * デバッグやモニタリングに使用
     */
    fun getStats(): Map<String, Any> {
        val videoInfoStats = videoInfoCache.stats()
        val nicoadHistoryStats = nicoadHistoryCache.stats()

        return mapOf(
            "videoInfo" to
                mapOf(
                    "hitRate" to videoInfoStats.hitRate(),
                    "missRate" to videoInfoStats.missRate(),
                    "hitCount" to videoInfoStats.hitCount(),
                    "missCount" to videoInfoStats.missCount(),
                    "loadSuccessCount" to videoInfoStats.loadSuccessCount(),
                    "loadFailureCount" to videoInfoStats.loadFailureCount(),
                    "evictionCount" to videoInfoStats.evictionCount(),
                    "estimatedSize" to videoInfoCache.estimatedSize(),
                ),
            "nicoadHistory" to
                mapOf(
                    "hitRate" to nicoadHistoryStats.hitRate(),
                    "missRate" to nicoadHistoryStats.missRate(),
                    "hitCount" to nicoadHistoryStats.hitCount(),
                    "missCount" to nicoadHistoryStats.missCount(),
                    "loadSuccessCount" to nicoadHistoryStats.loadSuccessCount(),
                    "loadFailureCount" to nicoadHistoryStats.loadFailureCount(),
                    "evictionCount" to nicoadHistoryStats.evictionCount(),
                    "estimatedSize" to nicoadHistoryCache.estimatedSize(),
                ),
        )
    }
}
