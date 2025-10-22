package net.numa08.niconico_advertiser_list2.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.forms.TextInput
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.numa08.niconico_advertiser_list2.models.NicoadHistory
import net.numa08.niconico_advertiser_list2.models.VideoInfo
import net.numa08.niconico_advertiser_list2.utils.VideoIdExtractor
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.w3c.fetch.RequestInit

/**
 * 動画検索ページ
 */
@Page("/video-search")
@Composable
fun VideoSearchPage() {
    val scope = rememberCoroutineScope()
    var videoInput by remember { mutableStateOf("") }
    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var nicoadHistoryList by remember { mutableStateOf<List<NicoadHistory>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(2.cssRem)
                .gap(1.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // タイトル
        SpanText(
            "ニコニコ動画 広告主リスト取得",
            modifier =
                Modifier
                    .fontSize(2.cssRem)
                    .fontWeight(FontWeight.Bold)
                    .margin(bottom = 1.cssRem),
        )

        // 入力フォーム
        Column(
            modifier =
                Modifier
                    .width(100.percent)
                    .maxWidth(600.px)
                    .gap(1.cssRem),
        ) {
            SpanText("動画URLまたは動画IDを入力してください")

            TextInput(
                text = videoInput,
                onTextChange = { videoInput = it },
                placeholder = "例: sm12345678 または https://www.nicovideo.jp/watch/sm12345678",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(0.5.cssRem),
            )

            // ボタン群
            Column(modifier = Modifier.gap(0.5.cssRem)) {
                Button(
                    onClick = {
                        errorMessage = null
                        val videoId = VideoIdExtractor.extractVideoId(videoInput)
                        if (videoId != null) {
                            isLoading = true
                            scope.launch {
                                val (result, error) = fetchVideoInfo(videoId)
                                isLoading = false
                                videoInfo = result
                                errorMessage = error
                            }
                        } else {
                            errorMessage = "有効な動画IDまたはURLを入力してください"
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SpanText(if (isLoading) "読み込み中..." else "動画情報を取得")
                }

                Button(
                    onClick = {
                        errorMessage = null
                        val videoId = VideoIdExtractor.extractVideoId(videoInput)
                        if (videoId != null) {
                            isLoading = true
                            scope.launch {
                                val (result, error) = fetchNicoadHistory(videoId)
                                isLoading = false
                                nicoadHistoryList = result
                                errorMessage = error
                            }
                        } else {
                            errorMessage = "有効な動画IDまたはURLを入力してください"
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SpanText(if (isLoading) "読み込み中..." else "広告履歴を取得")
                }
            }

            // エラーメッセージ
            errorMessage?.let { error ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(1.cssRem)
                            .backgroundColor(
                                org.jetbrains.compose.web.css
                                    .Color("#ffebee"),
                            ).borderRadius(4.px),
                ) {
                    SpanText(
                        error,
                        modifier =
                            Modifier.color(
                                org.jetbrains.compose.web.css
                                    .Color("#c62828"),
                            ),
                    )
                }
            }
        }

        // 動画情報表示
        videoInfo?.let { info ->
            Column(
                modifier =
                    Modifier
                        .width(100.percent)
                        .maxWidth(600.px)
                        .padding(1.cssRem)
                        .backgroundColor(
                            org.jetbrains.compose.web.css
                                .Color("#f5f5f5"),
                        ).borderRadius(8.px)
                        .gap(0.5.cssRem),
            ) {
                SpanText("動画情報", modifier = Modifier.fontSize(1.5.cssRem).fontWeight(FontWeight.Bold))
                SpanText("動画ID: ${info.videoId}")
                SpanText("タイトル: ${info.title}")
                SpanText("サムネイル: ${info.thumbnail}")
            }
        }

        // 広告履歴表示
        nicoadHistoryList?.let { historyList ->
            Column(
                modifier =
                    Modifier
                        .width(100.percent)
                        .maxWidth(600.px)
                        .padding(1.cssRem)
                        .backgroundColor(
                            org.jetbrains.compose.web.css
                                .Color("#f5f5f5"),
                        ).borderRadius(8.px)
                        .gap(0.5.cssRem),
            ) {
                SpanText("広告履歴", modifier = Modifier.fontSize(1.5.cssRem).fontWeight(FontWeight.Bold))
                SpanText("広告件数: ${historyList.size}")
                SpanText("総貢献度: ${historyList.sumOf { it.contribution }}pt")

                historyList.forEach { history ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(0.5.cssRem)
                                .backgroundColor(
                                    org.jetbrains.compose.web.css
                                        .Color("#ffffff"),
                                ).borderRadius(4.px),
                    ) {
                        Column(modifier = Modifier.gap(0.25.cssRem)) {
                            SpanText("${history.advertiserName} (ID: ${history.nicoadId})")
                            history.userId?.let { userId ->
                                SpanText("ユーザーID: $userId")
                            }
                            SpanText("広告ポイント: ${history.adPoint}pt")
                            SpanText("貢献度: ${history.contribution}pt")
                            SpanText("開始: ${history.startedAt} / 終了: ${history.endedAt}")
                            history.message?.let { msg ->
                                SpanText("メッセージ: $msg")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 動画情報を取得する
 */
private suspend fun fetchVideoInfo(videoId: String): Pair<VideoInfo?, String?> =
    try {
        val response =
            window
                .fetch(
                    "/api/video/info?videoId=$videoId",
                    RequestInit(method = "GET"),
                ).await()

        if (response.ok) {
            val json = response.text().await()
            val videoInfo = Json.decodeFromString<VideoInfo>(json)
            Pair(videoInfo, null)
        } else {
            Pair(null, "エラー: ${response.status} ${response.statusText}")
        }
    } catch (e: Exception) {
        Pair(null, "エラー: ${e.message}")
    }

/**
 * 広告履歴を取得する
 */
private suspend fun fetchNicoadHistory(videoId: String): Pair<List<NicoadHistory>?, String?> =
    try {
        val response =
            window
                .fetch(
                    "/api/video/nicoad-history?videoId=$videoId",
                    RequestInit(method = "GET"),
                ).await()

        if (response.ok) {
            val json = response.text().await()
            val historyList = Json.decodeFromString<List<NicoadHistory>>(json)
            Pair(historyList, null)
        } else {
            Pair(null, "エラー: ${response.status} ${response.statusText}")
        }
    } catch (e: Exception) {
        Pair(null, "エラー: ${e.message}")
    }
