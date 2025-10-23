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
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.forms.TextInput
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.numa08.niconico_advertiser_list2.models.NicoadHistory
import net.numa08.niconico_advertiser_list2.models.VideoInfo
import net.numa08.niconico_advertiser_list2.util.VideoIdExtractor
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

    // フォーマット設定
    var honorific by remember { mutableStateOf("様") }
    var customHonorific by remember { mutableStateOf("") }
    var displayFormat by remember { mutableStateOf("すべて表示") }
    var charsPerLine by remember { mutableStateOf("50") }

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

            // 検索ボタン
            Button(
                onClick = {
                    errorMessage = null
                    val videoId = VideoIdExtractor.extractVideoId(videoInput)
                    if (videoId != null) {
                        isLoading = true
                        scope.launch {
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
                    } else {
                        errorMessage = "有効な動画IDまたはURLを入力してください"
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SpanText(if (isLoading) "読み込み中..." else "動画情報と広告履歴を取得")
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
                            .backgroundColor(org.jetbrains.compose.web.css.Color("#ffffff"))
                            .borderRadius(4.px)
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
                val formattedList = formatAdvertiserList(historyList, honorific, customHonorific, displayFormat, charsPerLineValue)

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(1.cssRem)
                            .backgroundColor(org.jetbrains.compose.web.css.Color("#ffffff"))
                            .borderRadius(4.px),
                ) {
                    Pre(
                        attrs = {
                            style {
                                property("font-family", "monospace")
                                property("font-size", "0.9rem")
                                property("white-space", "pre-wrap")
                                property("margin", "0")
                            }
                        },
                    ) {
                        Text(formattedList)
                    }
                }

                // クリップボードにコピーボタン
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
 * 広告主リストをフォーマットする
 */
private fun formatAdvertiserList(
    historyList: List<NicoadHistory>,
    honorific: String,
    customHonorific: String,
    displayFormat: String,
    charsPerLine: Int,
): String {
    // 敬称を決定
    val suffix = if (honorific == "カスタム") customHonorific else honorific

    // 広告主名のリストを作成
    val names = historyList.map { it.advertiserName }

    // 表示形式に応じて処理
    val processedNames =
        when (displayFormat) {
            "すべて表示" -> names
            "すべて表示（逆順）" -> names.reversed()
            "同じ名前をまとめる" -> names.distinct()
            "同じ名前をまとめる（逆順）" -> names.distinct().reversed()
            else -> names
        }

    // 敬称を付ける
    val namesWithHonorific = processedNames.map { "$it$suffix" }

    // 1行の文字数で改行（人名は途中で改行しない）
    val lines = mutableListOf<String>()
    var currentLine = ""

    for (name in namesWithHonorific) {
        val testLine = if (currentLine.isEmpty()) name else "$currentLine　$name"

        if (testLine.length <= charsPerLine) {
            currentLine = testLine
        } else {
            // 現在の行を追加して新しい行を開始
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            currentLine = name
        }
    }


    // 最後の行を追加
    if (currentLine.isNotEmpty()) {
        lines.add(currentLine)
    }

    return lines.joinToString("\n")
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
