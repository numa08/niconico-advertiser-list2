package net.numa08.niconico_advertiser_list2.models

import kotlinx.serialization.Serializable

/**
 * ユーザーの投稿動画情報
 *
 * @property videoId 動画ID (例: sm43234567)
 * @property title 動画タイトル
 * @property thumbnail サムネイルURL
 * @property published 投稿日時 (ISO8601形式)
 * @property link 動画URL
 */
@Serializable
data class UserVideo(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val published: String,
    val link: String,
)
