package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.navigation.UpdateHistoryMode
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.forms.TextInput
import com.varabyte.kobweb.silk.components.text.SpanText
import net.numa08.niconico_advertiser_list2.util.VideoIdExtractor
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
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
                val videoId = VideoIdExtractor.extractVideoId(videoInput)
                if (videoId != null) {
                    // バリデーション成功、ナビゲーション（履歴を置き換え）
                    onError(null)
                    ctx.router.navigateTo(
                        "/advertisers/$videoId",
                        updateHistoryMode = UpdateHistoryMode.REPLACE,
                    )
                } else {
                    // バリデーション失敗
                    onError("有効な動画IDまたはURLを入力してください")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            SpanText("検索")
        }
    }
}
