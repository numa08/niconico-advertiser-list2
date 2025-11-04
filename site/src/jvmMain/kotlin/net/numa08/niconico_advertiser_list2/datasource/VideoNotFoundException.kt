package net.numa08.niconico_advertiser_list2.datasource

/**
 * 動画が見つからない場合の例外
 */
class VideoNotFoundException(
    message: String,
) : Exception(message)
