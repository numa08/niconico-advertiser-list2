package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.TextDecorationLine
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.text.SpanText
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Text

/**
 * アプリケーションフッター
 */
@Composable
fun Footer(modifier: Modifier = Modifier) {
    val theme = Theme.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(topBottom = 2.cssRem, leftRight = 2.cssRem)
                .backgroundColor(theme.surface)
                .borderTop(1.px, org.jetbrains.compose.web.css.LineStyle.Solid, theme.border)
                .gap(0.5.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 注意書き
        SpanText(
            text = "このツールは非公式のファンツールです",
            modifier =
                Modifier
                    .fontSize(0.9.cssRem)
                    .color(theme.onSurfaceVariant),
        )

        // Twitterアカウントへのリンク
        A(
            href = "https://x.com/numa_radio",
            attrs =
                Modifier
                    .fontSize(0.9.cssRem)
                    .color(theme.primary)
                    .textDecorationLine(TextDecorationLine.None)
                    .toAttrs {
                        target(ATarget.Blank)
                        attr("rel", "noopener noreferrer")
                    },
        ) {
            Text("@numa_radio")
        }

        // GitHubリポジトリへのリンク
        A(
            href = "https://github.com/numa08/niconico-advertiser-list2",
            attrs =
                Modifier
                    .fontSize(0.9.cssRem)
                    .color(theme.primary)
                    .textDecorationLine(TextDecorationLine.None)
                    .toAttrs {
                        target(ATarget.Blank)
                        attr("rel", "noopener noreferrer")
                    },
        ) {
            Text("GitHub")
        }
    }
}
