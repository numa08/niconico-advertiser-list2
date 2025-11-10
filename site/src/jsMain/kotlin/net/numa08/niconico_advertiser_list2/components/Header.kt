package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.ObjectFit
import com.varabyte.kobweb.compose.css.TextDecorationLine
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.icons.fa.FaUser
import com.varabyte.kobweb.silk.components.icons.fa.IconSize
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import net.numa08.niconico_advertiser_list2.util.UserPreferencesService
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Text

/**
 * アプリケーションヘッダー
 */
@Composable
fun Header(modifier: Modifier = Modifier) {
    val theme = Theme.current
    var userId by remember { mutableStateOf(UserPreferencesService.getUserId()) }
    var isDialogOpen by remember { mutableStateOf(false) }

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

        // ユーザーIDアイコン（常に表示）
        val currentUserId = userId
        if (currentUserId != null) {
            // ユーザーIDが設定されている場合はアイコン画像を表示
            Image(
                src = getUserIconUrl(currentUserId),
                alt = "User Icon",
                modifier =
                    Modifier
                        .size(32.px)
                        .margin(right = 1.cssRem)
                        .borderRadius(50.percent)
                        .cursor(Cursor.Pointer)
                        .objectFit(ObjectFit.Cover)
                        .onClick { isDialogOpen = true },
            )
        } else {
            // ユーザーIDが未設定の場合はFaUserアイコンを表示
            FaUser(
                size = IconSize.LG,
                modifier =
                    Modifier
                        .margin(right = 1.cssRem)
                        .color(theme.onSurfaceVariant)
                        .cursor(Cursor.Pointer)
                        .onClick { isDialogOpen = true },
            )
        }

        // テーマ切り替えボタン
        ColorModeToggle()
    }

    // ユーザーID設定ダイアログ
    UserIdSettingDialog(
        isOpen = isDialogOpen,
        onClose = { isDialogOpen = false },
        onSave = { newUserId ->
            userId = newUserId
        },
        onClear = {
            userId = null
        },
    )
}

/**
 * ユーザーアイコンのURLを生成
 *
 * @param userId ユーザーID
 * @return アイコンのURL
 */
private fun getUserIconUrl(userId: String): String {
    // ユーザーIDの最初の2桁（または1桁）をプレフィックスとして使用
    val prefix =
        if (userId.length >= 2) {
            userId.substring(0, 2)
        } else {
            userId.substring(0, 1)
        }
    return "https://secure-dcdn.cdn.nimg.jp/nicoaccount/usericon/$prefix/$userId.jpg"
}
