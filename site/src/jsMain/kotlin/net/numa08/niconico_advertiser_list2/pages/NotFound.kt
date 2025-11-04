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
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px

/**
 * 404 Not Foundページ
 */
@Page("/404")
@Composable
fun NotFoundPage() {
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

        // 検索フォーム
        VideoSearchForm(
            onError = { error -> errorMessage = error },
        )

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
                        .color(
                            org.jetbrains.compose.web.css
                                .Color("#999999"),
                        ),
            )

            SpanText(
                "動画が見つかりませんでした",
                modifier =
                    Modifier
                        .fontSize(1.5.cssRem)
                        .fontWeight(FontWeight.Bold),
            )

            SpanText(
                "指定された動画が存在しないか、URLが間違っている可能性があります",
                modifier =
                    Modifier
                        .fontSize(1.cssRem)
                        .color(
                            org.jetbrains.compose.web.css
                                .Color("#666666"),
                        ).textAlign(TextAlign.Center),
            )
        }
    }
}
