package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.ObjectFit
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.navigation.UpdateHistoryMode
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.text.SpanText
import net.numa08.niconico_advertiser_list2.models.UserVideo
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import org.jetbrains.compose.web.css.*
import kotlin.js.Date

/**
 * 動画カードコンポーネント（リスト表示用）
 *
 * @param video 動画情報
 */
@Composable
fun VideoCard(video: UserVideo) {
    val ctx = rememberPageContext()
    val theme = Theme.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .borderRadius(8.px)
                .backgroundColor(theme.surfaceVariant)
                .padding(1.cssRem)
                .gap(1.cssRem)
                .cursor(Cursor.Pointer)
                .onClick {
                    // 広告者ページへ遷移
                    ctx.router.navigateTo(
                        "/advertisers/${video.videoId}",
                        updateHistoryMode = UpdateHistoryMode.PUSH,
                    )
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // サムネイル
        Image(
            src = video.thumbnail,
            alt = video.title,
            modifier =
                Modifier
                    .width(160.px)
                    .height(90.px)
                    .borderRadius(4.px)
                    .objectFit(ObjectFit.Cover),
        )

        // 動画情報
        Column(
            modifier =
                Modifier
                    .flexGrow(1)
                    .gap(0.5.cssRem),
        ) {
            // タイトル
            SpanText(
                video.title,
                modifier =
                    Modifier
                        .fontSize(1.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .color(theme.onSurface)
                        .lineHeight(1.4),
            )

            // 動画ID
            SpanText(
                video.videoId,
                modifier =
                    Modifier
                        .fontSize(0.85.cssRem)
                        .color(theme.onSurfaceVariant),
            )

            // 投稿日時
            SpanText(
                formatDate(video.published),
                modifier =
                    Modifier
                        .fontSize(0.8.cssRem)
                        .color(theme.onSurfaceVariant),
            )
        }
    }
}

/**
 * ISO8601形式の日時を読みやすい形式に変換
 */
private fun formatDate(isoString: String): String =
    try {
        val date = Date(isoString)
        val year = date.getFullYear()
        val month = date.getMonth() + 1
        val day = date.getDate()
        "${year}年${month}月${day}日"
    } catch (e: Exception) {
        isoString
    }
