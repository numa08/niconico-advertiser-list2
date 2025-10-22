package net.numa08.niconico_advertiser_list2.datasource.response

import kotlinx.serialization.Serializable

/**
 * ニコニコ動画情報（内部レスポンス用）
 */
@Serializable
data class NiconicoVideoInformationResponse(
    val videoId: String,
    val title: String,
    val thumbnail: String,
)
