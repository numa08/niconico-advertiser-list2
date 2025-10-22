package net.numa08.niconico_advertiser_list2.models

import kotlinx.serialization.Serializable

/**
 * ニコニコ動画の基本情報
 *
 * @property thumbnail サムネイルURL（JVM側ではjava.net.URLとして扱う）
 */
@Serializable
data class VideoInfo(
    val videoId: String,
    val title: String,
    val thumbnail: String
)
