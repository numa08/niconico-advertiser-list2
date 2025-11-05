package net.numa08.niconico_advertiser_list2.theme

import org.jetbrains.compose.web.css.CSSColorValue

/**
 * アプリケーションテーマを定義するインターフェース
 */
interface Theme {
    // Background Colors
    val background: CSSColorValue
    val surface: CSSColorValue
    val surfaceVariant: CSSColorValue

    // Text Colors
    val onBackground: CSSColorValue
    val onSurface: CSSColorValue
    val onSurfaceVariant: CSSColorValue

    // Primary Colors
    val primary: CSSColorValue
    val onPrimary: CSSColorValue
    val primaryContainer: CSSColorValue
    val onPrimaryContainer: CSSColorValue

    // Error Colors
    val error: CSSColorValue
    val onError: CSSColorValue
    val errorContainer: CSSColorValue
    val onErrorContainer: CSSColorValue

    // Border Colors
    val border: CSSColorValue
    val borderVariant: CSSColorValue

    // Interactive Colors
    val hover: CSSColorValue
    val pressed: CSSColorValue

    companion object {
    }
}

/**
 * ライトテーマの実装
 */
object LightTheme : Theme {
    override val background = ColorPalette.white
    override val surface = ColorPalette.gray50
    override val surfaceVariant = ColorPalette.gray100

    override val onBackground = ColorPalette.gray900
    override val onSurface = ColorPalette.gray900
    override val onSurfaceVariant = ColorPalette.gray600

    override val primary = ColorPalette.blue600
    override val onPrimary = ColorPalette.white
    override val primaryContainer = ColorPalette.blue100
    override val onPrimaryContainer = ColorPalette.blue900

    override val error = ColorPalette.red600
    override val onError = ColorPalette.white
    override val errorContainer = ColorPalette.red100
    override val onErrorContainer = ColorPalette.red700

    override val border = ColorPalette.gray300
    override val borderVariant = ColorPalette.gray200

    override val hover = ColorPalette.withAlpha(ColorPalette.gray900, 0.05)
    override val pressed = ColorPalette.withAlpha(ColorPalette.gray900, 0.1)
}

/**
 * ダークテーマの実装
 */
object DarkTheme : Theme {
    override val background = ColorPalette.gray900
    override val surface = ColorPalette.gray800
    override val surfaceVariant = ColorPalette.gray700

    override val onBackground = ColorPalette.gray100
    override val onSurface = ColorPalette.gray100
    override val onSurfaceVariant = ColorPalette.gray300

    override val primary = ColorPalette.blue500
    override val onPrimary = ColorPalette.gray900
    override val primaryContainer = ColorPalette.blue900
    override val onPrimaryContainer = ColorPalette.blue200

    override val error = ColorPalette.red500
    override val onError = ColorPalette.gray900
    override val errorContainer = ColorPalette.red900
    override val onErrorContainer = ColorPalette.red200

    override val border = ColorPalette.gray700
    override val borderVariant = ColorPalette.gray600

    override val hover = ColorPalette.withAlpha(ColorPalette.gray100, 0.05)
    override val pressed = ColorPalette.withAlpha(ColorPalette.gray100, 0.1)
}
