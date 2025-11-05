package net.numa08.niconico_advertiser_list2.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.varabyte.kobweb.silk.theme.colors.ColorMode

/**
 * 現在のテーマを提供するCompositionLocal
 */
val LocalTheme =
    staticCompositionLocalOf<Theme> {
        error("No Theme provided")
    }

/**
 * 現在のテーマを取得する拡張プロパティ
 */
val Theme.Companion.current: Theme
    @Composable
    get() = LocalTheme.current

/**
 * ColorModeに基づいてテーマを取得
 */
fun ColorMode.toTheme(): Theme =
    when (this) {
        ColorMode.LIGHT -> LightTheme
        ColorMode.DARK -> DarkTheme
    }

/**
 * アプリケーションテーマプロバイダー
 *
 * ColorModeに基づいて適切なテーマを提供します。
 *
 * @param content テーマを使用するコンテンツ
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colorMode = ColorMode.current
    val theme = colorMode.toTheme()

    CompositionLocalProvider(
        LocalTheme provides theme,
    ) {
        content()
    }
}
