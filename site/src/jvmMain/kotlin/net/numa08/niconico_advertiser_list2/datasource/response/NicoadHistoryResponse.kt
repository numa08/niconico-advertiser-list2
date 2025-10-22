package net.numa08.niconico_advertiser_list2.datasource.response

import kotlinx.serialization.Serializable

/**
 * 広告主リストデータ（APIレスポンス用）
 * jsでLongを使うとエラーになるのでIntにしている
 */
@Serializable
data class NicoadHistoryResponse(
    /** 広告主名 */
    val advertiserName: String,
    /** 広告主ID */
    val nicoadId: Int,
    /** 広告ポイント */
    val adPoint: Int,
    /** 広告貢献度 */
    val contribution: Int,
    /** 広告開始日時 たぶん JST */
    val startedAt: Int,
    /** 広告終了日時 たぶん JST */
    val endedAt: Int,
    /** ユーザーID */
    val userId: Int? = null,
    /** メッセージ */
    val message: String? = null,
)
