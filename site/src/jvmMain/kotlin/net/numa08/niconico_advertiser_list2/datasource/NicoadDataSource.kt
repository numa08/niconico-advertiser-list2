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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.numa08.niconico_advertiser_list2.datasource.response.NicoadHistoryResponse
import net.numa08.niconico_advertiser_list2.datasource.response.NicoadResponse
import net.numa08.niconico_advertiser_list2.datasource.response.NiconicoVideoInformationResponse
import kotlin.math.ceil
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
        private fun nicoadHistoryURL(
            videoId: String,
            offset: Int,
            limit: Int,
        ): String = "https://api.nicoad.nicovideo.jp/v1/contents/video/$videoId/histories?offset=$offset&limit=$limit"

        private val json = Json { ignoreUnknownKeys = true }
    }

    /** ページネーションされたニコニコ動画広告履歴データを取得し、一つのリストでレスポンスする */
    suspend fun getNicoadHistories(videoId: String): Result<List<NicoadHistoryResponse>> =
        coroutineScope {
            val limit = 100
            val apiUrl = nicoadHistoryURL(videoId, 0, limit)
            // 1度取得して、ページ数を取得する
            val response = runCatching { httpClient.get(apiUrl).body<NicoadResponse>() }
            if (response.isFailure) {
                return@coroutineScope Result.failure(response.exceptionOrNull()!!)
            }
            val total = response.getOrThrow().data.count
            val totalPage = ceil(total.toDouble() / limit).toInt()
            // ページ数分リクエストを送る
            val requests =
                (0 until totalPage).map { index ->
                    val offset = index * limit
                    val pageUrl = nicoadHistoryURL(videoId, offset, limit)
                    // ランダムに遅延させる
                    val randomDelay = Random.nextInt(10, 101)
                    async {
                        delay(randomDelay.toLong())
                        runCatching { httpClient.get(pageUrl).body<NicoadResponse>() }
                    }
                }
            val responses = requests.awaitAll()
            // すべてのデータを連結する
            val allData = responses.mapNotNull { it.getOrNull()?.data?.histories }.flatten()
            val errors = responses.mapNotNull { it.exceptionOrNull() }
            // エラーがあったらエラーを返す
            if (errors.isNotEmpty()) {
                return@coroutineScope Result.failure(errors.first())
            }
            Result.success(allData)
        }

    /** 動画ページにアクセスし、htmlのヘッダー要素から動画情報を取得する */
    suspend fun getVideoInformation(videoId: String): Result<NiconicoVideoInformationResponse> {
        val url = "https://www.nicovideo.jp/watch/$videoId"
        val response =
            runCatching {
                httpClient
                    .get(url) {
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
                    }.bodyAsText()
            }.map { body ->
                val metadata = Ksoup.parseMetaData(body)
                val userId = extractUserIdFromHtml(body)
                NiconicoVideoInformationResponse(
                    videoId = videoId,
                    title = metadata.ogTitle ?: metadata.title ?: "",
                    thumbnail = metadata.ogImage ?: "",
                    userId = userId,
                )
            }
        return response
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
}
