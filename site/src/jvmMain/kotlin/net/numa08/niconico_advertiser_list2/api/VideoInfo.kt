package net.numa08.niconico_advertiser_list2.api

import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import com.varabyte.kobweb.api.http.setBody
import net.numa08.niconico_advertiser_list2.datasource.HttpClientFactory
import net.numa08.niconico_advertiser_list2.datasource.NicoadDataSource
import net.numa08.niconico_advertiser_list2.datasource.VideoNotFoundException
import net.numa08.niconico_advertiser_list2.models.VideoInfo
import net.numa08.niconico_advertiser_list2.util.RequestSemaphore

/**
 * 動画情報取得API
 * エンドポイント: GET /api/video/info?videoId={videoId}
 */
@Api(routeOverride = "video/info")
suspend fun getVideoInfo(ctx: ApiContext) {
    val videoId = ctx.req.params["videoId"]

    if (videoId.isNullOrBlank()) {
        ctx.res.status = 400
        return
    }

    val dataSource = NicoadDataSource(HttpClientFactory.httpClient)
    val result =
        RequestSemaphore.withLimit {
            dataSource.getVideoInformation(videoId)
        }

    result.fold(
        onSuccess = { response ->
            val videoInfo =
                VideoInfo(
                    videoId = response.videoId,
                    title = response.title,
                    thumbnail = response.thumbnail,
                    userId = response.userId,
                )
            ctx.res.status = 200
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
