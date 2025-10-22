package net.numa08.niconico_advertiser_list2.api

import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import com.varabyte.kobweb.api.http.setBody
import net.numa08.niconico_advertiser_list2.models.VideoInfo

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

    // TODO: 実際のニコニコ動画APIを呼び出す実装に置き換える
    val mockVideoInfo =
        VideoInfo(
            videoId = videoId,
            title = "【モック】サンプル動画タイトル - $videoId",
            thumbnail = "https://example.com/thumbnail.jpg",
        )

    ctx.res.status = 200
    ctx.res.setBody(mockVideoInfo)
}
