package net.numa08.niconico_advertiser_list2.api

import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import com.varabyte.kobweb.api.http.setBody
import net.numa08.niconico_advertiser_list2.cache.VideoCacheService
import net.numa08.niconico_advertiser_list2.datasource.VideoNotFoundException

/**
 * 動画情報取得API
 * エンドポイント: GET /api/video/info?videoId={videoId}&refresh={true|false}
 *
 * クエリパラメータ:
 * - videoId: 動画ID（必須）
 * - refresh: キャッシュを強制更新する場合はtrue（オプション、デフォルト: false）
 */
@Api(routeOverride = "video/info")
fun getVideoInfo(ctx: ApiContext) {
    val videoId = ctx.req.params["videoId"]
    val forceRefresh = ctx.req.params["refresh"]?.toBoolean() ?: false

    if (videoId.isNullOrBlank()) {
        ctx.res.status = 400
        return
    }

    val result =
        if (forceRefresh) {
            VideoCacheService.refreshVideoInfo(videoId)
        } else {
            VideoCacheService.getVideoInfo(videoId)
        }

    result.fold(
        onSuccess = { cached ->
            val videoInfo =
                cached.videoInfo.copy(
                    cachedAt = cached.cachedAt.toString(),
                    fromCache = !forceRefresh,
                )
            ctx.res.status = 200
            // 圧縮を無効化
            ctx.res.setBody(videoInfo)
        },
        onFailure = { error ->
            when (error) {
                is VideoNotFoundException -> {
                    ctx.res.status = 404
                    ctx.res.setBody(mapOf("error" to (error.message ?: "Video not found")))
                }
                else -> {
                    ctx.res.status = 500
                    ctx.res.setBody(mapOf("error" to (error.message ?: "Unknown error")))
                }
            }
        },
    )
}
