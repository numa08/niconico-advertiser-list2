package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.text.SpanText
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import org.jetbrains.compose.web.css.cssRem

/**
 * ページネーションコンポーネント
 *
 * @param currentPage 現在のページ番号
 * @param hasNext 次のページがあるか
 * @param onPrevious 前のページボタンのクリックハンドラ
 * @param onNext 次のページボタンのクリックハンドラ
 */
@Composable
fun Pagination(
    currentPage: Int,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val theme = Theme.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .gap(1.cssRem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 前へボタン
        Button(
            onClick = { onPrevious() },
            enabled = currentPage > 1,
            modifier =
                Modifier
                    .padding(topBottom = 0.75.cssRem, leftRight = 1.5.cssRem)
                    .backgroundColor(if (currentPage > 1) theme.primary else theme.surfaceVariant)
                    .color(if (currentPage > 1) theme.onPrimary else theme.onSurfaceVariant),
        ) {
            SpanText("← 前へ")
        }

        // ページ番号表示
        SpanText(
            "ページ $currentPage",
            modifier =
                Modifier
                    .flexGrow(1)
                    .textAlign(TextAlign.Center)
                    .fontSize(1.cssRem)
                    .fontWeight(FontWeight.Medium)
                    .color(theme.onSurface),
        )

        // 次へボタン
        Button(
            onClick = { onNext() },
            enabled = hasNext,
            modifier =
                Modifier
                    .padding(topBottom = 0.75.cssRem, leftRight = 1.5.cssRem)
                    .backgroundColor(if (hasNext) theme.primary else theme.surfaceVariant)
                    .color(if (hasNext) theme.onPrimary else theme.onSurfaceVariant),
        ) {
            SpanText("次へ →")
        }
    }
}
