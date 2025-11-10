package net.numa08.niconico_advertiser_list2.models

import kotlinx.serialization.Serializable

/**
 * ユーザー動画一覧のレスポンス
 *
 * @property userId ユーザーID
 * @property page 現在のページ番号
 * @property videos 動画リスト
 * @property videosCount このページの動画件数
 * @property hasNext 次のページがあるか（推定）
 * @property feedUpdated feedの更新日時
 * @property cachedAt キャッシュ日時
 * @property fromCache キャッシュから取得したか
 */
@Serializable
data class UserVideosResponse(
    val userId: String,
    val page: Int,
    val videos: List<UserVideo>,
    val videosCount: Int,
    val hasNext: Boolean,
    val feedUpdated: String? = null,
    val cachedAt: String? = null,
    val fromCache: Boolean = false,
)
