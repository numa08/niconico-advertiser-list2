package net.numa08.niconico_advertiser_list2.api

import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import com.varabyte.kobweb.api.http.setBody
import net.numa08.niconico_advertiser_list2.models.NicoadHistory

/**
 * ニコニ広告履歴取得API
 * エンドポイント: GET /api/video/nicoad-history?videoId={videoId}
 */
@Api(routeOverride = "video/nicoad-history")
suspend fun getVideoNicoadHistory(ctx: ApiContext) {
    val videoId = ctx.req.params["videoId"]

    if (videoId.isNullOrBlank()) {
        ctx.res.status = 400
        return
    }

    // TODO: 実際のニコニコ動画APIを呼び出して広告履歴を取得する実装に置き換える
    val mockHistory = listOf(
        NicoadHistory(
            advertiserName = "広告主A",
            nicoadId = 1001,
            adPoint = 10000,
            contribution = 10000,
            startedAt = 1705712400, // 2025-01-20 10:00:00 JST (仮のUnixタイムスタンプ)
            endedAt = 1705798800,   // 2025-01-21 10:00:00 JST
            userId = 12345,
            message = "応援しています！"
        ),
        NicoadHistory(
            advertiserName = "広告主B",
            nicoadId = 1002,
            adPoint = 5000,
            contribution = 5000,
            startedAt = 1705626000, // 2025-01-19 10:00:00 JST
            endedAt = 1705712400,
            userId = 12346,
            message = null
        ),
        NicoadHistory(
            advertiserName = "広告主C",
            nicoadId = 1003,
            adPoint = 20000,
            contribution = 20000,
            startedAt = 1705539600, // 2025-01-18 10:00:00 JST
            endedAt = 1705626000,
            userId = null,
            message = "素晴らしい動画でした"
        )
    )

    ctx.res.status = 200
    ctx.res.setBody(mockHistory)
}
