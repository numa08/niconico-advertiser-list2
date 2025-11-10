package net.numa08.niconico_advertiser_list2.api

import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import com.varabyte.kobweb.api.http.setBodyText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.numa08.niconico_advertiser_list2.cache.VideoCacheService
import net.numa08.niconico_advertiser_list2.datasource.VideoNotFoundException
import net.numa08.niconico_advertiser_list2.models.UserVideosResponse

/**
 * ユーザーの投稿動画一覧を取得するAPIエンドポイント
 *
 * GET /api/user/videos?userId={userId}&page={page}&refresh={true|false}
 *
 * @param userId ユーザーID（必須）
 * @param page ページ番号（オプション、デフォルト: 1）
 * @param refresh キャッシュを無視して再取得（オプション、デフォルト: false）
 */
@Api("user/videos")
suspend fun userVideos(ctx: ApiContext) {
    try {
        // パラメータ取得
        val userId =
            ctx.req.params["userId"]
                ?: run {
                    ctx.res.status = 400
                    ctx.res.setBodyText("Missing required parameter: userId")
                    return
                }

        val page = ctx.req.params["page"]?.toIntOrNull() ?: 1
        val refresh = ctx.req.params["refresh"]?.toBoolean() ?: false

        // パラメータバリデーション
        if (page < 1) {
            ctx.res.status = 400
            ctx.res.setBodyText("Invalid parameter: page must be >= 1")
            return
        }

        // ユーザーIDのバリデーション（数字のみ）
        if (!userId.all { it.isDigit() }) {
            ctx.res.status = 400
            ctx.res.setBodyText("Invalid parameter: userId must be numeric")
            return
        }

        // データ取得
        val result =
            if (refresh) {
                VideoCacheService.refreshUserVideos(userId, page)
            } else {
                VideoCacheService.getUserVideos(userId, page)
            }

        result.fold(
            onSuccess = { cached ->
                // hasNextの判定（次のページを先読みして判定）
                val nextPageResult = VideoCacheService.getUserVideos(userId, page + 1)
                val hasNext = nextPageResult.getOrNull()?.videosCount?.let { it > 0 } ?: false

                val response =
                    UserVideosResponse(
                        userId = cached.userId,
                        page = cached.page,
                        videos = cached.videos,
                        videosCount = cached.videosCount,
                        hasNext = hasNext,
                        feedUpdated = cached.feedUpdated,
                        cachedAt = cached.cachedAt.toString(),
                        fromCache = !refresh,
                    )

                ctx.res.status = 200
                ctx.res.setBodyText(Json.encodeToString(response))
            },
            onFailure = { error ->
                when (error) {
                    is VideoNotFoundException -> {
                        ctx.res.status = 404
                        ctx.res.setBodyText("User not found: $userId")
                    }
                    else -> {
                        ctx.res.status = 500
                        ctx.res.setBodyText("Internal server error: ${error.message}")
                    }
                }
            },
        )
    } catch (e: Exception) {
        ctx.res.status = 500
        ctx.res.setBodyText("Internal server error: ${e.message}")
    }
}
