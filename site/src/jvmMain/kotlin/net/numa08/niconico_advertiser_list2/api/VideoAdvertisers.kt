package net.numa08.niconico_advertiser_list2.api

import com.varabyte.kobweb.api.Api
import com.varabyte.kobweb.api.ApiContext
import com.varabyte.kobweb.api.http.setBody
import net.numa08.niconico_advertiser_list2.datasource.HttpClientFactory
import net.numa08.niconico_advertiser_list2.datasource.NicoadDataSource
import net.numa08.niconico_advertiser_list2.models.NicoadHistory
import net.numa08.niconico_advertiser_list2.util.RequestSemaphore

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

    val dataSource = NicoadDataSource(HttpClientFactory.httpClient)
    val result =
        RequestSemaphore.withLimit {
            dataSource.getNicoadHistories(videoId)
        }

    result.fold(
        onSuccess = { responses ->
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
            ctx.res.status = 200
            ctx.res.setBody(histories)
        },
        onFailure = { error ->
            ctx.res.status = 500
            ctx.res.setBody(mapOf("error" to (error.message ?: "Unknown error")))
        },
    )
}
