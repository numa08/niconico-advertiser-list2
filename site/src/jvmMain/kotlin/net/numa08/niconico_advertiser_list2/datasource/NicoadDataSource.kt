package net.numa08.niconico_advertiser_list2.datasource

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.numa08.niconico_advertiser_list2.datasource.response.NicoadHistoryResponse
import net.numa08.niconico_advertiser_list2.datasource.response.NicoadResponse
import net.numa08.niconico_advertiser_list2.datasource.response.NiconicoVideoInformationResponse
import kotlin.random.Random

/**
 * JSON-LDスキーマのVideoObject用データクラス（部分的）
 */
@Serializable
private data class JsonLdVideoObject(
    val author: JsonLdAuthor? = null,
)

/**
 * JSON-LDスキーマのAuthor用データクラス
 */
@Serializable
private data class JsonLdAuthor(
    val url: String? = null,
)

/**
 * ニコニコ動画のAPIやWebページにアクセスして情報を取得するデータソース
 *
 * @param httpClient KtorのHttpClient
 */
class NicoadDataSource(
    private val httpClient: HttpClient,
) : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.IO + job

    companion object {
        private const val HISTORY_PAGE_LIMIT = 100
        private const val MIN_PAGE_DELAY_MS = 10
        private const val MAX_PAGE_DELAY_MS = 101

        private fun nicoadHistoryURL(
            videoId: String,
            offsetId: Int?,
            limit: Int,
        ): String =
            buildString {
                append("https://api.koken.nicovideo.jp/v1/userperspective/contents/nicoad/video/")
                append(videoId)
                append("/histories?limit=")
                append(limit)
                if (offsetId != null) {
                    append("&offsetId=")
                    append(offsetId)
                }
            }

        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * ニコニコ動画広告履歴データをすべて取得し、一つのリストでレスポンスする
     *
     * APIはoffsetIdによるカーソル方式のため、前ページ最後の履歴IDを渡しながら
     * nextCountが0になるまで順次取得する
     */
    suspend fun getNicoadHistories(videoId: String): Result<List<NicoadHistoryResponse>> =
        runCatching {
            val allHistories = mutableListOf<NicoadHistoryResponse>()
            var offsetId: Int? = null
            while (true) {
                val httpResponse = httpClient.get(nicoadHistoryURL(videoId, offsetId, HISTORY_PAGE_LIMIT))
                if (httpResponse.status.value == 404) {
                    throw VideoNotFoundException("Video not found: $videoId")
                }
                val page = httpResponse.body<NicoadResponse>()
                val items = page.data.histories
                allHistories +=
                    items.map { item ->
                        NicoadHistoryResponse(
                            advertiserName = item.supporterName,
                            nicoadId = item.id,
                            adPoint = item.point,
                            contribution = item.contribution,
                            startedAt = item.startedAt,
                            endedAt = item.endedAt,
                            userId = item.supporterId,
                        )
                    }
                if (page.data.nextCount <= 0 || items.isEmpty()) {
                    break
                }
                offsetId = items.last().id
                // 連続アクセスによる負荷を避けるためランダムに遅延させる
                delay(Random.nextInt(MIN_PAGE_DELAY_MS, MAX_PAGE_DELAY_MS).toLong())
            }
            allHistories.toList()
        }

    /** 動画ページにアクセスし、htmlのヘッダー要素から動画情報を取得する */
    suspend fun getVideoInformation(videoId: String): Result<NiconicoVideoInformationResponse> {
        val url = "https://www.nicovideo.jp/watch/$videoId"
        return runCatching {
            val httpResponse =
                httpClient.get(url) {
                    headers {
                        append(
                            HttpHeaders.Accept,
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                        )
                        append(
                            HttpHeaders.AcceptLanguage,
                            "ja-JP,ja;q=0.9,en-US;q=0.8,en;q=0.7,de-DE;q=0.6,de;q=0.5,fr-FR;q=0.4,fr;q=0.3,zh-TW;q=0.2,zh;q=0.1,ko-KR;q=0.1,ko;q=0.1",
                        )
                    }
                }

            // 404チェック
            if (httpResponse.status.value == 404) {
                throw VideoNotFoundException("Video not found: $videoId")
            }

            val body = httpResponse.bodyAsText()
            val metadata = Ksoup.parseMetaData(body)
            val userId = extractUserIdFromHtml(body)
            NiconicoVideoInformationResponse(
                videoId = videoId,
                title = metadata.ogTitle ?: metadata.title ?: "",
                thumbnail = metadata.ogImage ?: "",
                userId = userId,
            )
        }
    }

    /**
     * HTMLから投稿者のユーザーIDを抽出する
     *
     * 優先順位:
     * 1. JSON-LDスキーマのauthor.urlから抽出
     * 2. 正規表現で/user/{userId}パターンを検索
     */
    private fun extractUserIdFromHtml(html: String): String {
        // 方法1: Ksoupを使ってJSON-LDスキーマから抽出
        try {
            val document = Ksoup.parse(html)
            val jsonLdScripts = document.select("script[type=application/ld+json]")

            for (script in jsonLdScripts) {
                val jsonText = script.data()
                try {
                    val videoObject = json.decodeFromString<JsonLdVideoObject>(jsonText)
                    val authorUrl = videoObject.author?.url
                    if (authorUrl != null) {
                        // URLから/user/{userId}パターンを抽出
                        val userIdPattern = Regex("""/user/(\d+)""")
                        val match = userIdPattern.find(authorUrl)
                        if (match != null) {
                            return match.groupValues[1]
                        }
                    }
                } catch (e: Exception) {
                    // このJSON-LDブロックはVideoObjectではない、次を試す
                    continue
                }
            }
        } catch (e: Exception) {
            // パース失敗、フォールバックへ
        }

        // 方法2: HTML全体から/user/{userId}パターンを検索（フォールバック）
        val userLinkPattern = Regex("""/user/(\d+)""")
        val userMatch = userLinkPattern.find(html)
        if (userMatch != null) {
            return userMatch.groupValues[1]
        }

        // 見つからない場合は空文字列を返す
        return ""
    }

    /**
     * ユーザーの投稿動画一覧をAtom feedで取得
     *
     * @param userId ユーザーID
     * @param page ページ番号（デフォルト: 1）
     * @return Atom feed XML文字列
     * @throws VideoNotFoundException ユーザーが存在しない場合
     */
    suspend fun fetchUserVideosFeed(
        userId: String,
        page: Int = 1,
    ): String {
        val url = "https://www.nicovideo.jp/user/$userId/video?rss=atom&page=$page"

        return runCatching {
            val response =
                httpClient.get(url) {
                    headers {
                        append(
                            HttpHeaders.Accept,
                            "application/atom+xml,application/xml;q=0.9,*/*;q=0.8",
                        )
                        append(
                            HttpHeaders.AcceptLanguage,
                            "ja-JP,ja;q=0.9,en-US;q=0.8,en;q=0.7",
                        )
                        append(
                            HttpHeaders.UserAgent,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                        )
                    }
                }

            when (response.status.value) {
                404 -> throw VideoNotFoundException("User not found: $userId")
                in 200..299 -> response.bodyAsText()
                else -> throw Exception("Failed to fetch user videos: ${response.status}")
            }
        }.getOrElse { error ->
            when (error) {
                is VideoNotFoundException -> throw error
                else -> throw Exception("Failed to fetch user videos feed: ${error.message}", error)
            }
        }
    }
}
