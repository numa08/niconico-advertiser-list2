package net.numa08.niconico_advertiser_list2.util

/**
 * ニコニコ動画のURLから動画IDを抽出するユーティリティ
 */
object VideoIdExtractor {
    /**
     * URLから動画IDを抽出する
     *
     * @param url ニコニコ動画のURL
     * @return 抽出された動画ID。抽出できない場合はnull
     */
    fun extractVideoId(url: String): String? {
        // 空文字チェック
        if (url.isBlank()) return null

        // URLのパース
        val parsedUrl =
            try {
                parseUrl(url)
            } catch (e: Exception) {
                return null
            }

        // ドメインチェック（nicovideo.jpまたはそのサブドメイン）
        if (!isNiconicoVideoDomain(parsedUrl.host)) {
            return null
        }

        // パスから動画IDを抽出
        return extractVideoIdFromPath(parsedUrl.path)
    }

    /**
     * ドメインがnicovideo.jpまたはそのサブドメインかチェック
     */
    private fun isNiconicoVideoDomain(host: String): Boolean {
        val lowerHost = host.lowercase()
        return lowerHost == "nicovideo.jp" || lowerHost.endsWith(".nicovideo.jp")
    }

    /**
     * パスから動画IDを抽出
     * パターン: /watch/{videoId}
     * videoIdはsmから始まる数字
     */
    private fun extractVideoIdFromPath(path: String): String? {
        // /watch/{videoId} のパターンをチェック
        val watchPrefix = "/watch/"
        if (!path.startsWith(watchPrefix)) {
            return null
        }

        // /watch/の後の部分を取得
        val videoId =
            path
                .substring(watchPrefix.length)
                .split("?")
                .first()
                .split("#")
                .first()

        // smから始まる数字のパターンをチェック
        if (!isValidVideoId(videoId)) {
            return null
        }

        return videoId
    }

    /**
     * 動画IDが有効な形式（smから始まる数字）かチェック
     */
    private fun isValidVideoId(videoId: String): Boolean {
        if (!videoId.startsWith("sm")) {
            return false
        }

        // sm以降が数字かチェック
        val numberPart = videoId.substring(2)
        if (numberPart.isEmpty()) {
            return false
        }

        return numberPart.all { it.isDigit() }
    }

    /**
     * 簡易的なURLパーサー
     */
    private fun parseUrl(url: String): ParsedUrl {
        // スキームの抽出
        val schemeEnd = url.indexOf("://")
        if (schemeEnd == -1) {
            throw IllegalArgumentException("Invalid URL: missing scheme")
        }

        val scheme = url.substring(0, schemeEnd)
        val remaining = url.substring(schemeEnd + 3)

        // ホストとパスの分離
        val pathStart = remaining.indexOf('/')
        val host: String
        val path: String

        if (pathStart == -1) {
            host = remaining
            path = "/"
        } else {
            host = remaining.substring(0, pathStart)
            path = remaining.substring(pathStart)
        }

        return ParsedUrl(scheme, host, path)
    }

    /**
     * パース済みURL
     */
    private data class ParsedUrl(
        val scheme: String,
        val host: String,
        val path: String,
    )
}
