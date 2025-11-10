package net.numa08.niconico_advertiser_list2.util

/**
 * ニコニコ動画のユーザーURLからユーザーIDを抽出するユーティリティ
 */
object UserIdExtractor {
    /**
     * URLまたはユーザーIDからユーザーIDを抽出する
     *
     * @param input ニコニコ動画のユーザーURLまたはユーザーID（例: 753685）
     * @return 抽出されたユーザーID。抽出できない場合はnull
     *
     * サポートするパターン:
     * - 直接ID: "753685"
     * - ユーザーページURL: "https://www.nicovideo.jp/user/753685"
     * - ユーザー動画URL: "https://www.nicovideo.jp/user/753685/video"
     * - RSS URL: "https://www.nicovideo.jp/user/753685/video?rss=atom"
     */
    fun extractUserId(input: String): String? {
        // 空文字チェック
        if (input.isBlank()) return null

        val trimmedInput = input.trim()

        // まず、直接ユーザーIDとして有効かチェック（数字のみ）
        if (isValidUserId(trimmedInput)) {
            return trimmedInput
        }

        // URLとしてパース
        val parsedUrl =
            try {
                parseUrl(trimmedInput)
            } catch (e: Exception) {
                return null
            }

        // ドメインチェック（nicovideo.jpまたはそのサブドメイン）
        if (!isNiconicoVideoDomain(parsedUrl.host)) {
            return null
        }

        // パスからユーザーIDを抽出
        return extractUserIdFromPath(parsedUrl.path)
    }

    /**
     * ドメインがnicovideo.jpまたはそのサブドメインかチェック
     */
    private fun isNiconicoVideoDomain(host: String): Boolean {
        val lowerHost = host.lowercase()
        return lowerHost == "nicovideo.jp" || lowerHost.endsWith(".nicovideo.jp")
    }

    /**
     * パスからユーザーIDを抽出
     * パターン: /user/{userId} または /user/{userId}/video
     */
    private fun extractUserIdFromPath(path: String): String? {
        // /user/{userId} のパターンをチェック
        val userPrefix = "/user/"
        if (!path.startsWith(userPrefix)) {
            return null
        }

        // /user/の後の部分を取得
        val userId =
            path
                .substring(userPrefix.length)
                .split("/")
                .first()
                .split("?")
                .first()
                .split("#")
                .first()

        // 数字のみのパターンをチェック
        if (!isValidUserId(userId)) {
            return null
        }

        return userId
    }

    /**
     * ユーザーIDが有効な形式（数字のみ）かチェック
     */
    private fun isValidUserId(userId: String): Boolean {
        if (userId.isEmpty()) {
            return false
        }
        return userId.all { it.isDigit() }
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
