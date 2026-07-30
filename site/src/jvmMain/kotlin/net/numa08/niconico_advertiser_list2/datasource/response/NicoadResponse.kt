package net.numa08.niconico_advertiser_list2.datasource.response

import kotlinx.serialization.Serializable

/**
 * ニコニ広告履歴API（koken userperspective）のレスポンス
 *
 * エンドポイント:
 * https://api.koken.nicovideo.jp/v1/userperspective/contents/nicoad/video/{videoId}/histories
 *
 * 旧 api.nicoad.nicovideo.jp/v1 は廃止されたため、現行のニコニコ動画視聴ページが
 * 利用しているこのAPIを使用する。offset方式ではなくoffsetId（カーソル）方式で
 * ページネーションする。
 */
@Serializable
data class NicoadResponse(
    val meta: Meta,
    val data: Data,
) {
    @Serializable
    data class Meta(
        val status: Int,
    )

    @Serializable
    data class Data(
        /** 続きとして取得できる履歴件数。0なら最終ページ */
        val nextCount: Int,
        /** 動画の総広告ポイント */
        val totalPoint: Long = 0,
        val histories: List<History>,
    )

    @Serializable
    data class History(
        /** 広告履歴ID。次ページ取得時のoffsetIdに使う */
        val id: Int,
        /** 広告主のユーザーID。匿名の場合はnull */
        val supporterId: Int? = null,
        /** 広告主名 */
        val supporterName: String,
        /** 広告主のアイコンURL */
        val supporterThumbnailUrl: String? = null,
        /** 広告ポイント */
        val point: Int,
        /** 広告貢献度 */
        val contribution: Int,
        /** 広告開始日時（エポック秒） */
        val startedAt: Int,
        /** 広告終了日時（エポック秒） */
        val endedAt: Int,
        /** 現在広告中かどうか */
        val isCurrentlyAdvertising: Boolean = false,
    )
}
