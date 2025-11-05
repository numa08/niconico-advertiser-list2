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
import net.numa08.niconico_advertiser_list2.models.NicoadHistoryResponse
import net.numa08.niconico_advertiser_list2.models.VideoInfo
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLPreElement
import org.w3c.fetch.RequestInit
import kotlin.js.Date

/**
 * キャッシュの動作を定義するenum
 */
enum class CacheBehavior {
    /** キャッシュを利用する */
    USE_CACHE,

    /** キャッシュを強制的に更新する */
    FORCE_REFRESH,
}

/**
 * 広告主リスト表示ページ（動的ルーティング）
 * /advertisers/{videoId}
 */
@Page("/advertisers/{videoId}")
@Composable
fun AdvertisersPage() {
    val ctx = rememberPageContext()
    val scope = rememberCoroutineScope()
    val theme = Theme.current

    // URLパラメータから取得
    val videoId = ctx.route.params["videoId"] ?: ""

    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var nicoadHistoryList by remember { mutableStateOf<List<NicoadHistory>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

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
            var has404 = false

            // 動画情報と広告履歴を並列取得
            val job1 =
                launch {
                    val (result, statusCode, error) = fetchVideoInfo(videoId, CacheBehavior.USE_CACHE)
                    videoInfo = result
                    if (statusCode == 404) {
                        has404 = true
                    }
                    error?.let { errors.add(it) }
                }

            val job2 =
                launch {
                    val (result, statusCode, error) = fetchNicoadHistory(videoId, CacheBehavior.USE_CACHE)
                    nicoadHistoryList = result
                    if (statusCode == 404) {
                        has404 = true
                    }
                    error?.let { errors.add(it) }
                }

            // 両方の完了を待つ
            job1.join()
            job2.join()

            isLoading = false

            // 404の場合は404ページにリダイレクト
            if (has404) {
                ctx.router.navigateTo("/404")
            } else {
                errorMessage = errors.takeIf { it.isNotEmpty() }?.joinToString("; ")
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.cssRem, bottom = 4.cssRem, leftRight = 2.cssRem)
                .gap(2.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // タイトル
        SpanText(
            text = "ニコニコ動画 広告主リスト取得",
            modifier =
                Modifier
                    .fontSize(2.5.cssRem)
                    .fontWeight(FontWeight.Bold)
                    .textAlign(com.varabyte.kobweb.compose.css.TextAlign.Center)
                    .color(theme.onBackground),
        )

        // 説明文
        SpanText(
            text = "動画URLまたは動画IDを入力すると、広告主のリストを整形して表示します。感謝メッセージの作成などにご利用ください。",
            modifier =
                Modifier
                    .fontSize(1.1.cssRem)
                    .textAlign(com.varabyte.kobweb.compose.css.TextAlign.Center)
                    .maxWidth(600.px)
                    .color(theme.onSurfaceVariant),
        )

        // 検索フォーム
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .maxWidth(600.px),
        ) {
            VideoSearchForm(
                initialValue = videoId,
                onError = { error -> errorMessage = error },
            )
        }

        // リフレッシュボタン（データが存在する場合のみ表示）
        if (videoId.isNotEmpty() && (videoInfo != null || nicoadHistoryList != null)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .maxWidth(600.px)
                        .gap(1.cssRem),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isRefreshing = true
                            errorMessage = null
                            val errors = mutableListOf<String>()

                            // 動画情報と広告履歴を並列で強制更新
                            val job1 =
                                launch {
                                    val (result, _, error) = fetchVideoInfo(videoId, CacheBehavior.FORCE_REFRESH)
                                    videoInfo = result
                                    error?.let { errors.add(it) }
                                }

                            val job2 =
                                launch {
                                    val (result, _, error) = fetchNicoadHistory(videoId, CacheBehavior.FORCE_REFRESH)
                                    nicoadHistoryList = result
                                    error?.let { errors.add(it) }
                                }

                            job1.join()
                            job2.join()

                            isRefreshing = false
                            errorMessage = errors.takeIf { it.isNotEmpty() }?.joinToString("; ")
                        }
                    },
                    enabled = !isLoading && !isRefreshing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SpanText(if (isRefreshing) "更新中..." else "データを更新")
                }
            }

            // キャッシュ情報
            videoInfo?.let { info ->
                if (info.cachedAt != null) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .maxWidth(600.px)
                                .padding(0.5.cssRem)
                                .backgroundColor(theme.primaryContainer)
                                .borderRadius(4.px),
                    ) {
                        SpanText(
                            "キャッシュ: ${if (info.fromCache) "利用中" else "更新済"} (${formatDateTime(info.cachedAt)})",
                            modifier =
                                Modifier
                                    .fontSize(0.85.cssRem)
                                    .color(theme.onPrimaryContainer),
                        )
                    }
                }
            }
        }

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
                        .borderRadius(8.px)
                        .backgroundColor(theme.errorContainer)
                        .border(1.px, org.jetbrains.compose.web.css.LineStyle.Solid, theme.error),
            ) {
                Column(modifier = Modifier.gap(0.5.cssRem)) {
                    SpanText(
                        text = "エラー",
                        modifier = Modifier.fontWeight(FontWeight.Bold).color(theme.onErrorContainer),
                    )
                    SpanText(
                        text = error,
                        modifier = Modifier.fontSize(0.9.cssRem).color(theme.onErrorContainer),
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
                        .backgroundColor(theme.surfaceVariant)
                        .borderRadius(8.px)
                        .gap(1.cssRem),
            ) {
                SpanText(
                    "動画情報",
                    modifier =
                        Modifier
                            .fontSize(1.5.cssRem)
                            .fontWeight(FontWeight.Bold)
                            .color(theme.onSurface),
                )

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
                                    .color(theme.onSurface)
                                    .lineHeight(1.4),
                        )
                        SpanText(
                            "動画ID: ${info.videoId}",
                            modifier =
                                Modifier
                                    .fontSize(0.9.cssRem)
                                    .color(theme.onSurfaceVariant),
                        )
                        SpanText(
                            "投稿者ID: ${info.userId}",
                            modifier =
                                Modifier
                                    .fontSize(0.9.cssRem)
                                    .color(theme.onSurfaceVariant),
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
                        .backgroundColor(theme.surfaceVariant)
                        .borderRadius(8.px)
                        .gap(1.cssRem),
            ) {
                SpanText(
                    "広告主リスト",
                    modifier =
                        Modifier
                            .fontSize(1.5.cssRem)
                            .fontWeight(FontWeight.Bold)
                            .color(theme.onSurface),
                )
                SpanText("広告件数: ${historyList.size}", modifier = Modifier.color(theme.onSurface))
                SpanText("総貢献度: ${historyList.sumOf { it.contribution }}pt", modifier = Modifier.color(theme.onSurface))

                // フォーマット設定
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(1.cssRem)
                            .backgroundColor(theme.surface)
                            .borderRadius(4.px)
                            .gap(1.cssRem),
                ) {
                    SpanText(
                        "表示設定",
                        modifier =
                            Modifier
                                .fontWeight(FontWeight.Bold)
                                .color(theme.onSurface),
                    )

                    // 敬称設定
                    Column(modifier = Modifier.gap(0.5.cssRem)) {
                        SpanText("敬称:", modifier = Modifier.color(theme.onSurface))
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
                        SpanText("リスト表示形式:", modifier = Modifier.color(theme.onSurface))
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
                        SpanText("1行の文字数:", modifier = Modifier.color(theme.onSurface))
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
                            property("background-color", theme.surface.toString())
                            property("color", theme.onSurface.toString())
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
 * @param videoId 動画ID
 * @param cacheBehavior キャッシュの動作
 * @return Triple(データ, HTTPステータスコード, エラーメッセージ)
 */
private suspend fun fetchVideoInfo(
    videoId: String,
    cacheBehavior: CacheBehavior = CacheBehavior.USE_CACHE,
): Triple<VideoInfo?, Int, String?> =
    try {
        val url =
            "/api/video/info?videoId=$videoId" +
                if (cacheBehavior == CacheBehavior.FORCE_REFRESH) "&refresh=true" else ""
        val response =
            window
                .fetch(url, RequestInit())
                .await()

        if (response.ok) {
            val json = response.text().await()
            val videoInfo = Json.decodeFromString<VideoInfo>(json)
            Triple(videoInfo, response.status.toInt(), null)
        } else {
            Triple(null, response.status.toInt(), "動画情報の取得に失敗しました: ${response.statusText}")
        }
    } catch (e: Exception) {
        Triple(null, 500, "エラーが発生しました: ${e.message}")
    }

/**
 * 広告履歴を取得する
 * @param videoId 動画ID
 * @param cacheBehavior キャッシュの動作
 * @return Triple(データ, HTTPステータスコード, エラーメッセージ)
 */
private suspend fun fetchNicoadHistory(
    videoId: String,
    cacheBehavior: CacheBehavior = CacheBehavior.USE_CACHE,
): Triple<List<NicoadHistory>?, Int, String?> =
    try {
        val url =
            "/api/video/nicoad-history?videoId=$videoId" +
                if (cacheBehavior == CacheBehavior.FORCE_REFRESH) "&refresh=true" else ""
        val response =
            window
                .fetch(url, RequestInit())
                .await()

        if (response.ok) {
            val json = response.text().await()
            val historyResponse = Json.decodeFromString<NicoadHistoryResponse>(json)
            Triple(historyResponse.histories, response.status.toInt(), null)
        } else {
            Triple(null, response.status.toInt(), "広告履歴の取得に失敗しました: ${response.statusText}")
        }
    } catch (e: Exception) {
        Triple(null, 500, "エラーが発生しました: ${e.message}")
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

/**
 * ISO8601形式の日時文字列を人間が読みやすい形式に変換する
 * ブラウザのタイムゾーンでローカライズされた日時を返す
 *
 * @param isoString ISO8601形式の日時文字列（例: "2025-11-05T12:34:56.789Z"）
 * @return 読みやすい形式の日時文字列（例: "2025年11月5日 21:34:56"）
 */
private fun formatDateTime(isoString: String): String =
    try {
        val date = Date(isoString)
        val year = date.getFullYear()
        val month = date.getMonth() + 1 // 0-11なので+1
        val day = date.getDate()
        val hours = date.getHours().toString().padStart(2, '0')
        val minutes = date.getMinutes().toString().padStart(2, '0')
        val seconds = date.getSeconds().toString().padStart(2, '0')

        "${year}年${month}月${day}日 $hours:$minutes:$seconds"
    } catch (e: Exception) {
        isoString // エラー時は元の文字列を返す
    }
