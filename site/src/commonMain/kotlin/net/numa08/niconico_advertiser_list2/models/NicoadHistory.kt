package net.numa08.niconico_advertiser_list2.models

import kotlinx.serialization.Serializable

/**
 * ニコニ広告履歴
 */
@Serializable
data class NicoadHistory(
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
