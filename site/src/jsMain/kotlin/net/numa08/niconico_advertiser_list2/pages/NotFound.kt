package net.numa08.niconico_advertiser_list2.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.text.SpanText
import net.numa08.niconico_advertiser_list2.components.VideoSearchForm
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px

/**
 * 404 Not Foundページ
 */
@Page("/404")
@Composable
fun NotFoundPage() {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val theme = Theme.current

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

        // 検索フォーム
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .maxWidth(600.px),
        ) {
            VideoSearchForm(
                onError = { error -> errorMessage = error },
            )
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

        // 404メッセージ
        Column(
            modifier =
                Modifier
                    .width(100.cssRem)
                    .maxWidth(600.px)
                    .padding(top = 2.cssRem, bottom = 1.cssRem)
                    .gap(1.cssRem),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpanText(
                "404",
                modifier =
                    Modifier
                        .fontSize(4.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .color(theme.onSurfaceVariant),
            )

            SpanText(
                "動画が見つかりませんでした",
                modifier =
                    Modifier
                        .fontSize(1.5.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .color(theme.onBackground),
            )

            SpanText(
                "指定された動画が存在しないか、URLが間違っている可能性があります",
                modifier =
                    Modifier
                        .fontSize(1.cssRem)
                        .color(theme.onSurfaceVariant)
                        .textAlign(TextAlign.Center),
            )
        }
    }
}
