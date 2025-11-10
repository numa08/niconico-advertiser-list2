package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.navigation.UpdateHistoryMode
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.forms.TextInput
import com.varabyte.kobweb.silk.components.text.SpanText
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import net.numa08.niconico_advertiser_list2.util.GoogleAnalytics
import net.numa08.niconico_advertiser_list2.util.VideoIdExtractor
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px

/**
 * 動画検索フォームコンポーネント
 *
 * @param initialValue 初期入力値
 * @param onError エラー発生時のコールバック
 */
@Composable
fun VideoSearchForm(
    initialValue: String = "",
    onError: (String?) -> Unit = {},
) {
    val ctx = rememberPageContext()
    var videoInput by remember { mutableStateOf(initialValue) }
    val theme = Theme.current

    val handleSubmit = {
        val videoId = VideoIdExtractor.extractVideoId(videoInput)
        if (videoId != null) {
            onError(null)
            // 動画検索イベントを送信
            GoogleAnalytics.trackVideoSearch()
            ctx.router.navigateTo(
                "/advertisers/$videoId",
                updateHistoryMode = UpdateHistoryMode.REPLACE,
            )
        } else {
            onError("有効な動画IDまたはURLを入力してください")
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().gap(1.cssRem),
    ) {
        // ラベル
        SpanText(
            text = "動画URLまたは動画IDを入力",
            modifier =
                Modifier
                    .fontSize(0.9.cssRem)
                    .fontWeight(FontWeight.Medium)
                    .color(theme.onSurface),
        )

        // 入力とボタンを横並び
        Row(
            modifier = Modifier.fillMaxWidth().gap(0.5.cssRem),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 入力フィールド
            TextInput(
                text = videoInput,
                onTextChange = { videoInput = it },
                placeholder = "例: sm12345678 または https://...",
                modifier =
                    Modifier
                        .flexGrow(1)
                        .padding(0.75.cssRem)
                        .borderRadius(6.px)
                        .fontSize(1.cssRem),
                onCommit = { handleSubmit() },
            )

            // 検索ボタン
            Button(
                onClick = { handleSubmit() },
                modifier =
                    Modifier
                        .padding(topBottom = 0.75.cssRem, leftRight = 1.5.cssRem)
                        .borderRadius(6.px)
                        .backgroundColor(theme.primary)
                        .color(theme.onPrimary)
                        .fontWeight(FontWeight.Bold)
                        .fontSize(1.cssRem),
            ) {
                SpanText("検索")
            }
        }
    }
}
