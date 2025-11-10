package net.numa08.niconico_advertiser_list2.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.numa08.niconico_advertiser_list2.components.*
import net.numa08.niconico_advertiser_list2.models.UserVideosResponse
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import net.numa08.niconico_advertiser_list2.util.UserPreferencesService
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.w3c.fetch.RequestInit

/**
 * トップページ
 */
@Page("/")
@Composable
fun HomePage() {
    val theme = Theme.current
    val scope = rememberCoroutineScope()

    // 状態管理
    var userId by remember { mutableStateOf(UserPreferencesService.getUserId()) }
    var isDialogOpen by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var videosResponse by remember { mutableStateOf<UserVideosResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ユーザーIDが変更されたら動画を取得
    LaunchedEffect(userId, currentPage) {
        if (userId != null) {
            isLoading = true
            errorMessage = null

            scope.launch {
                try {
                    val response =
                        window.fetch(
                            "/api/user/videos?userId=$userId&page=$currentPage",
                            RequestInit(),
                        ).await()

                    if (response.ok) {
                        val json = response.text().await()
                        videosResponse = Json.decodeFromString<UserVideosResponse>(json)
                    } else {
                        errorMessage =
                            when (response.status.toInt()) {
                                404 -> "ユーザーが見つかりませんでした"
                                else -> "動画の取得に失敗しました: ${response.statusText}"
                            }
                        videosResponse = null
                    }
                } catch (e: Exception) {
                    errorMessage = "エラーが発生しました: ${e.message}"
                    videosResponse = null
                } finally {
                    isLoading = false
                }
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
                    .textAlign(TextAlign.Center)
                    .color(theme.onBackground),
        )

        // 説明文
        SpanText(
            text = "動画URLまたは動画IDを入力すると、広告主のリストを整形して表示します。感謝メッセージの作成などにご利用ください。",
            modifier =
                Modifier
                    .fontSize(1.1.cssRem)
                    .textAlign(TextAlign.Center)
                    .maxWidth(600.px)
                    .color(theme.onSurfaceVariant),
        )

        // ユーザーID設定状態による分岐
        if (userId == null) {
            // ユーザーID未設定時
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .maxWidth(600.px)
                        .gap(1.cssRem),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 動画検索フォーム
                VideoSearchForm(
                    onError = { error -> errorMessage = error },
                )

                // または
                SpanText(
                    "または",
                    modifier =
                        Modifier
                            .fontSize(1.cssRem)
                            .color(theme.onSurfaceVariant),
                )

                // 投稿者ID設定ボタン
                Button(
                    onClick = { isDialogOpen = true },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(1.cssRem)
                            .backgroundColor(theme.primaryContainer)
                            .color(theme.onPrimaryContainer),
                ) {
                    SpanText("投稿者IDを設定して自分の動画から選択")
                }

                SpanText(
                    "投稿者IDを設定すると、あなたの動画一覧から選択できます",
                    modifier =
                        Modifier
                            .fontSize(0.9.cssRem)
                            .textAlign(TextAlign.Center)
                            .color(theme.onSurfaceVariant),
                )
            }
        } else {
            // ユーザーID設定済み時
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .maxWidth(900.px)
                        .gap(1.5.cssRem),
            ) {
                // ローディング表示
                if (isLoading) {
                    SpanText(
                        "読み込み中...",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .textAlign(TextAlign.Center)
                                .fontSize(1.1.cssRem)
                                .color(theme.onSurface),
                    )
                }

                // エラーメッセージ
                errorMessage?.let { error ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(1.cssRem)
                                .borderRadius(8.px)
                                .backgroundColor(theme.errorContainer)
                                .border(1.px, LineStyle.Solid, theme.error),
                    ) {
                        SpanText(
                            error,
                            modifier =
                                Modifier
                                    .color(theme.onErrorContainer),
                        )
                    }
                }

                // 動画一覧
                videosResponse?.let { response ->
                    Column(modifier = Modifier.fillMaxWidth().gap(1.5.cssRem)) {
                        // 動画件数表示
                        SpanText(
                            "${response.videosCount}件の動画",
                            modifier =
                                Modifier
                                    .fontSize(1.1.cssRem)
                                    .fontWeight(FontWeight.Medium)
                                    .color(theme.onSurface),
                        )

                        if (response.videos.isEmpty()) {
                            // 空状態
                            SpanText(
                                "動画が見つかりませんでした",
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .textAlign(TextAlign.Center)
                                        .padding(2.cssRem)
                                        .fontSize(1.1.cssRem)
                                        .color(theme.onSurfaceVariant),
                            )
                        } else {
                            // 動画リスト
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .gap(1.cssRem),
                            ) {
                                response.videos.forEach { video ->
                                    VideoCard(video)
                                }
                            }

                            // ページネーション
                            Pagination(
                                currentPage = currentPage,
                                hasNext = response.hasNext,
                                onPrevious = {
                                    if (currentPage > 1) {
                                        currentPage--
                                        window.scrollTo(0.0, 0.0)
                                    }
                                },
                                onNext = {
                                    if (response.hasNext) {
                                        currentPage++
                                        window.scrollTo(0.0, 0.0)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // ユーザーID設定ダイアログ
    UserIdSettingDialog(
        isOpen = isDialogOpen,
        onClose = { isDialogOpen = false },
        onSave = { newUserId ->
            userId = newUserId
            currentPage = 1
            videosResponse = null
        },
        onClear = {
            userId = null
            currentPage = 1
            videosResponse = null
        },
    )
}
