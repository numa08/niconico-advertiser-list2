package net.numa08.niconico_advertiser_list2.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.ObjectFit
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.forms.TextInput
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.numa08.niconico_advertiser_list2.components.VideoSearchForm
import net.numa08.niconico_advertiser_list2.models.NicoadHistory
import net.numa08.niconico_advertiser_list2.models.VideoInfo
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text
import org.w3c.fetch.RequestInit

/**
 * 広告主リスト表示ページ（動的ルーティング）
 * /advertisers/{videoId}
 */
@Page("/advertisers/{videoId}")
@Composable
fun AdvertisersPage() {
    val ctx = rememberPageContext()
    val scope = rememberCoroutineScope()

    // URLパラメータから取得
    val videoId = ctx.route.params["videoId"] ?: ""

    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var nicoadHistoryList by remember { mutableStateOf<List<NicoadHistory>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // フォーマット設定
    var honorific by remember { mutableStateOf("様") }
    var customHonorific by remember { mutableStateOf("") }
    var displayFormat by remember { mutableStateOf("すべて表示") }
    var charsPerLine by remember { mutableStateOf("50") }

    // ページロード時に自動的にデータ取得
    LaunchedEffect(videoId) {
        if (videoId.isNotEmpty()) {
            isLoading = true
            val errors = mutableListOf<String>()

            // 動画情報と広告履歴を並列取得
            val job1 =
                launch {
                    val (result, error) = fetchVideoInfo(videoId)
                    videoInfo = result
                    error?.let { errors.add(it) }
                }

            val job2 =
                launch {
                    val (result, error) = fetchNicoadHistory(videoId)
                    nicoadHistoryList = result
                    error?.let { errors.add(it) }
                }

            // 両方の完了を待つ
            job1.join()
            job2.join()

            isLoading = false
            errorMessage = errors.takeIf { it.isNotEmpty() }?.joinToString("; ")
        }
    }

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
            "ニコニコ動画 広告主リスト",
            modifier =
                Modifier
                    .fontSize(2.cssRem)
                    .fontWeight(FontWeight.Bold)
                    .margin(bottom = 1.cssRem),
        )

        // 検索フォーム
        VideoSearchForm(
            initialValue = videoId,
            onError = { error -> errorMessage = error },
        )

        // ローディング表示
        if (isLoading) {
            SpanText("読み込み中...")
        }

        // エラーメッセージ
        errorMessage?.let { error ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .maxWidth(600.px)
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
                        .gap(1.cssRem),
            ) {
                SpanText("動画情報", modifier = Modifier.fontSize(1.5.cssRem).fontWeight(FontWeight.Bold))

                Row(
                    modifier = Modifier.fillMaxWidth().gap(1.cssRem),
                    verticalAlignment = Alignment.Top,
                ) {
                    // サムネイル画像
                    Image(
                        src = info.thumbnail,
                        alt = info.title,
                        modifier =
                            Modifier
                                .width(160.px)
                                .height(90.px)
                                .borderRadius(4.px)
                                .objectFit(ObjectFit.Cover),
                    )

                    // タイトルと動画ID
                    Column(
                        modifier = Modifier.fillMaxWidth().gap(0.5.cssRem),
                    ) {
                        SpanText(
                            info.title,
                            modifier =
                                Modifier
                                    .fontSize(1.1.cssRem)
                                    .fontWeight(FontWeight.Bold)
                                    .lineHeight(1.4),
                        )
                        SpanText(
                            "動画ID: ${info.videoId}",
                            modifier =
                                Modifier
                                    .fontSize(0.9.cssRem)
                                    .color(
                                        org.jetbrains.compose.web.css
                                            .Color("#666666"),
                                    ),
                        )
                        SpanText(
                            "投稿者ID: ${info.userId}",
                            modifier =
                                Modifier
                                    .fontSize(0.9.cssRem)
                                    .color(
                                        org.jetbrains.compose.web.css
                                            .Color("#666666"),
                                    ),
                        )
                    }
                }
            }
        }

        // 広告主リスト表示
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
                        .gap(1.cssRem),
            ) {
                SpanText("広告主リスト", modifier = Modifier.fontSize(1.5.cssRem).fontWeight(FontWeight.Bold))
                SpanText("広告件数: ${historyList.size}")
                SpanText("総貢献度: ${historyList.sumOf { it.contribution }}pt")

                // フォーマット設定
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(1.cssRem)
                            .backgroundColor(
                                org.jetbrains.compose.web.css
                                    .Color("#ffffff"),
                            ).borderRadius(4.px)
                            .gap(1.cssRem),
                ) {
                    SpanText("表示設定", modifier = Modifier.fontWeight(FontWeight.Bold))

                    // 敬称設定
                    Column(modifier = Modifier.gap(0.5.cssRem)) {
                        SpanText("敬称:")
                        Select(
                            attrs = {
                                style {
                                    property("padding", "0.5rem")
                                    property("border-radius", "4px")
                                    property("border", "1px solid #ccc")
                                }
                                onChange { event ->
                                    honorific = event.target.value
                                }
                            },
                        ) {
                            listOf("様", "さん", "氏", "ちゃん", "くん", "カスタム").forEach { option ->
                                Option(
                                    value = option,
                                    attrs = { if (option == honorific) selected() },
                                ) {
                                    Text(option)
                                }
                            }
                        }
                        if (honorific == "カスタム") {
                            TextInput(
                                text = customHonorific,
                                onTextChange = { customHonorific = it },
                                placeholder = "例: 殿",
                                modifier = Modifier.width(150.px).padding(0.5.cssRem),
                            )
                        }
                    }

                    // リスト表示形式
                    Column(modifier = Modifier.gap(0.5.cssRem)) {
                        SpanText("リスト表示形式:")
                        Select(
                            attrs = {
                                style {
                                    property("padding", "0.5rem")
                                    property("border-radius", "4px")
                                    property("border", "1px solid #ccc")
                                }
                                onChange { event ->
                                    displayFormat = event.target.value
                                }
                            },
                        ) {
                            listOf("すべて表示", "すべて表示（逆順）", "同じ名前をまとめる", "同じ名前をまとめる（逆順）").forEach { option ->
                                Option(
                                    value = option,
                                    attrs = { if (option == displayFormat) selected() },
                                ) {
                                    Text(option)
                                }
                            }
                        }
                    }

                    // 1行の文字数
                    Column(modifier = Modifier.gap(0.5.cssRem)) {
                        SpanText("1行の文字数:")
                        TextInput(
                            text = charsPerLine,
                            onTextChange = { charsPerLine = it },
                            placeholder = "50",
                            modifier = Modifier.width(100.px).padding(0.5.cssRem),
                        )
                    }
                }

                // フォーマット済み広告主リスト
                val charsPerLineValue = (charsPerLine.toIntOrNull() ?: 1).coerceAtLeast(1)
                val formattedList =
                    formatAdvertiserList(historyList, honorific, customHonorific, displayFormat, charsPerLineValue)

                Pre(
                    attrs = {
                        style {
                            property("background-color", "#ffffff")
                            property("padding", "1rem")
                            property("border-radius", "4px")
                            property("overflow-x", "auto")
                            property("white-space", "pre-wrap")
                            property("word-wrap", "break-word")
                        }
                    },
                ) {
                    Text(formattedList)
                }

                // コピーボタン
                Button(
                    onClick = {
                        window.navigator.clipboard.writeText(formattedList)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SpanText("クリップボードにコピー")
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
                .fetch("/api/video/info?videoId=$videoId", RequestInit())
                .await()

        if (response.ok) {
            val json = response.text().await()
            val videoInfo = Json.decodeFromString<VideoInfo>(json)
            Pair(videoInfo, null)
        } else {
            Pair(null, "動画情報の取得に失敗しました: ${response.statusText}")
        }
    } catch (e: Exception) {
        Pair(null, "エラーが発生しました: ${e.message}")
    }

/**
 * 広告履歴を取得する
 */
private suspend fun fetchNicoadHistory(videoId: String): Pair<List<NicoadHistory>?, String?> =
    try {
        val response =
            window
                .fetch("/api/video/nicoad-history?videoId=$videoId", RequestInit())
                .await()

        if (response.ok) {
            val json = response.text().await()
            val historyList = Json.decodeFromString<List<NicoadHistory>>(json)
            Pair(historyList, null)
        } else {
            Pair(null, "広告履歴の取得に失敗しました: ${response.statusText}")
        }
    } catch (e: Exception) {
        Pair(null, "エラーが発生しました: ${e.message}")
    }

/**
 * 広告主リストをフォーマットする
 */
private fun formatAdvertiserList(
    historyList: List<NicoadHistory>,
    honorific: String,
    customHonorific: String,
    displayFormat: String,
    charsPerLine: Int,
): String {
    val honorificSuffix =
        when (honorific) {
            "カスタム" -> customHonorific
            else -> honorific
        }

    val names =
        when (displayFormat) {
            "すべて表示" -> historyList.map { it.advertiserName + honorificSuffix }
            "すべて表示（逆順）" -> historyList.reversed().map { it.advertiserName + honorificSuffix }
            "同じ名前をまとめる" -> historyList.map { it.advertiserName }.distinct().map { it + honorificSuffix }
            "同じ名前をまとめる（逆順）" ->
                historyList
                    .reversed()
                    .map { it.advertiserName }
                    .distinct()
                    .map { it + honorificSuffix }
            else -> historyList.map { it.advertiserName + honorificSuffix }
        }

    return formatNamesWithLineBreaks(names, charsPerLine)
}

/**
 * 名前リストを指定文字数で改行してフォーマットする
 */
private fun formatNamesWithLineBreaks(
    names: List<String>,
    charsPerLine: Int,
): String {
    val result = StringBuilder()
    var currentLine = StringBuilder()
    var currentLength = 0

    names.forEachIndexed { index, name ->
        val separator = if (index < names.size - 1) "、" else ""
        val nameWithSeparator = name + separator

        if (currentLength + nameWithSeparator.length > charsPerLine && currentLine.isNotEmpty()) {
            result.append(currentLine.toString().trim()).append("\n")
            currentLine = StringBuilder()
            currentLength = 0
        }

        currentLine.append(nameWithSeparator)
        currentLength += nameWithSeparator.length
    }

    if (currentLine.isNotEmpty()) {
        result.append(currentLine.toString().trim())
    }

    return result.toString()
}
