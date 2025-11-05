package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.TextDecorationLine
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.toAttrs
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Text

/**
 * アプリケーションヘッダー
 */
@Composable
fun Header(modifier: Modifier = Modifier) {
    val theme = Theme.current

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(topBottom = 1.cssRem, leftRight = 2.cssRem)
                .backgroundColor(theme.surface)
                .borderBottom(1.px, org.jetbrains.compose.web.css.LineStyle.Solid, theme.border),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // アプリ名（トップページへのリンク）
        A(
            href = "/",
            attrs =
                Modifier
                    .fontSize(1.5.cssRem)
                    .fontWeight(FontWeight.Bold)
                    .color(theme.onSurface)
                    .textDecorationLine(TextDecorationLine.None)
                    .cursor(Cursor.Pointer)
                    .flexGrow(1)
                    .toAttrs(),
        ) {
            Text("ニコニコ動画 広告主リスト取得")
        }

        // テーマ切り替えボタン
        ColorModeToggle()
    }
}
