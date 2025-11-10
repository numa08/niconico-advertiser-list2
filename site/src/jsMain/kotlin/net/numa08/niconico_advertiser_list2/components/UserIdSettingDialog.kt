package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.forms.TextInput
import com.varabyte.kobweb.silk.components.text.SpanText
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import net.numa08.niconico_advertiser_list2.util.GoogleAnalytics
import net.numa08.niconico_advertiser_list2.util.UserIdExtractor
import net.numa08.niconico_advertiser_list2.util.UserPreferencesService
import org.jetbrains.compose.web.css.*
import com.varabyte.kobweb.compose.css.FontStyle

/**
 * ユーザーID設定ダイアログ
 *
 * @param isOpen ダイアログが開いているか
 * @param onClose ダイアログを閉じる際のコールバック
 * @param onSave ユーザーIDが保存された際のコールバック
 * @param onClear ユーザーIDが解除された際のコールバック
 */
@Composable
fun UserIdSettingDialog(
    isOpen: Boolean,
    onClose: () -> Unit,
    onSave: (String) -> Unit,
    onClear: (() -> Unit)? = null,
) {
    if (!isOpen) return

    val theme = Theme.current
    val currentUserId = UserPreferencesService.getUserId()
    var userIdInput by remember(isOpen) { mutableStateOf(currentUserId ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // モーダルオーバーレイ
    Box(
        modifier =
            Modifier
                .position(Position.Fixed)
                .top(0.px)
                .left(0.px)
                .width(100.percent)
                .height(100.percent)
                .backgroundColor(rgba(0, 0, 0, 0.5))
                .zIndex(1000)
                .onClick { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        // ダイアログコンテンツ
        Box(
            modifier =
                Modifier
                    .backgroundColor(theme.surface)
                    .borderRadius(12.px)
                    .padding(2.cssRem)
                    .maxWidth(500.px)
                    .width(90.percent)
                    .onClick { it.stopPropagation() }, // クリックイベントの伝播を停止
        ) {
            Column(modifier = Modifier.gap(1.5.cssRem)) {
                // タイトル
                SpanText(
                    "投稿者IDを設定",
                    modifier =
                        Modifier
                            .fontSize(1.5.cssRem)
                            .fontWeight(FontWeight.Bold)
                            .color(theme.onSurface),
                )

                // 説明文
                Column(modifier = Modifier.gap(0.5.cssRem)) {
                    SpanText(
                        "あなたのニコニコ動画のユーザーIDまたはユーザーページURLを入力してください",
                        modifier =
                            Modifier
                                .fontSize(0.9.cssRem)
                                .color(theme.onSurfaceVariant),
                    )
                    SpanText(
                        "例: 753685 または https://www.nicovideo.jp/user/753685",
                        modifier =
                            Modifier
                                .fontSize(0.85.cssRem)
                                .color(theme.onSurfaceVariant)
                                .fontStyle(FontStyle.Italic),
                    )
                }

                // 入力フィールド
                TextInput(
                    text = userIdInput,
                    onTextChange = {
                        userIdInput = it
                        errorMessage = null
                    },
                    placeholder = "ユーザーID または URL",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(0.75.cssRem)
                            .borderRadius(6.px)
                            .fontSize(1.cssRem),
                )

                // エラーメッセージ
                errorMessage?.let { error ->
                    SpanText(
                        error,
                        modifier =
                            Modifier
                                .fontSize(0.9.cssRem)
                                .color(theme.error),
                    )
                }

                // ボタン
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .gap(0.75.cssRem),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .gap(1.cssRem),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { onClose() },
                            modifier =
                                Modifier
                                    .flexGrow(1)
                                    .padding(0.75.cssRem)
                                    .backgroundColor(theme.surfaceVariant)
                                    .color(theme.onSurfaceVariant),
                        ) {
                            SpanText("キャンセル")
                        }

                        Button(
                            onClick = {
                                val userId = UserIdExtractor.extractUserId(userIdInput)
                                if (userId != null) {
                                    UserPreferencesService.setUserId(userId)
                                    // ユーザーID設定イベントを送信
                                    GoogleAnalytics.trackUserIdSet()
                                    onSave(userId)
                                    onClose()
                                } else {
                                    errorMessage = "有効なユーザーIDまたはURLを入力してください"
                                }
                            },
                            modifier =
                                Modifier
                                    .flexGrow(1)
                                    .padding(0.75.cssRem)
                                    .backgroundColor(theme.primary)
                                    .color(theme.onPrimary)
                                    .fontWeight(FontWeight.Bold),
                        ) {
                            SpanText("保存")
                        }
                    }

                    // 解除ボタン（ユーザーIDが設定されている場合のみ表示）
                    if (currentUserId != null && onClear != null) {
                        Button(
                            onClick = {
                                UserPreferencesService.clearUserId()
                                // ユーザーID解除イベントを送信
                                GoogleAnalytics.trackUserIdClear()
                                onClear()
                                onClose()
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(0.75.cssRem)
                                    .backgroundColor(theme.error)
                                    .color(theme.onError),
                        ) {
                            SpanText("ユーザーID設定を解除")
                        }
                    }
                }
            }
        }
    }
}
