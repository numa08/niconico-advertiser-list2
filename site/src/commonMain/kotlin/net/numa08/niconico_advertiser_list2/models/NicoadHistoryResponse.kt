package net.numa08.niconico_advertiser_list2.models

import kotlinx.serialization.Serializable

/**
 * ニコニ広告履歴のレスポンス
 *
 * @property histories 広告履歴のリスト
 * @property cachedAt キャッシュされた日時（ISO8601形式）
 * @property fromCache キャッシュから取得されたかどうか
 * @property nextOffsetId 継続カーソル。サーバーが1リクエストの取得上限
 *   （サブリクエスト制限対策で最大40ページ）に達した場合のみ非null。
 *   クライアントはこの値を offsetId パラメータに渡して続きを取得する
 */
@Serializable
data class NicoadHistoryResponse(
    val histories: List<NicoadHistory>,
    val cachedAt: String? = null,
    val fromCache: Boolean = false,
    val nextOffsetId: Int? = null,
)
