package net.numa08.niconico_advertiser_list2.theme

import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.rgb
import org.jetbrains.compose.web.css.rgba

/**
 * アプリケーションで使用するカラーパレット
 */
object ColorPalette {
    // Primary Colors (Blue)
    val blue50 = rgb(239, 246, 255)
    val blue100 = rgb(219, 234, 254)
    val blue200 = rgb(191, 219, 254)
    val blue300 = rgb(147, 197, 253)
    val blue400 = rgb(96, 165, 250)
    val blue500 = rgb(59, 130, 246)
    val blue600 = rgb(37, 99, 235)
    val blue700 = rgb(29, 78, 216)
    val blue800 = rgb(30, 64, 175)
    val blue900 = rgb(30, 58, 138)

    // Gray Colors
    val gray50 = rgb(249, 250, 251)
    val gray100 = rgb(243, 244, 246)
    val gray200 = rgb(229, 231, 235)
    val gray300 = rgb(209, 213, 219)
    val gray400 = rgb(156, 163, 175)
    val gray500 = rgb(107, 114, 128)
    val gray600 = rgb(75, 85, 99)
    val gray700 = rgb(55, 65, 81)
    val gray800 = rgb(31, 41, 55)
    val gray900 = rgb(17, 24, 39)

    // Red Colors (Error)
    val red50 = rgb(254, 242, 242)
    val red100 = rgb(254, 226, 226)
    val red200 = rgb(254, 202, 202)
    val red300 = rgb(252, 165, 165)
    val red400 = rgb(248, 113, 113)
    val red500 = rgb(239, 68, 68)
    val red600 = rgb(220, 38, 38)
    val red700 = rgb(185, 28, 28)
    val red800 = rgb(153, 27, 27)
    val red900 = rgb(127, 29, 29)

    // Green Colors (Success)
    val green50 = rgb(240, 253, 244)
    val green100 = rgb(220, 252, 231)
    val green200 = rgb(187, 247, 208)
    val green300 = rgb(134, 239, 172)
    val green400 = rgb(74, 222, 128)
    val green500 = rgb(34, 197, 94)
    val green600 = rgb(22, 163, 74)
    val green700 = rgb(21, 128, 61)
    val green800 = rgb(22, 101, 52)
    val green900 = rgb(20, 83, 45)

    // Semantic Colors - White/Black
    val white = rgb(255, 255, 255)
    val black = rgb(0, 0, 0)

    // Transparency helpers
    fun withAlpha(
        color: CSSColorValue,
        alpha: Double,
    ): CSSColorValue {
        // CSSColorValueからRGB値を抽出するのは難しいため、
        // 直接rgba関数を使える形で提供
        return when (color) {
            blue500 -> rgba(59, 130, 246, alpha)
            blue600 -> rgba(37, 99, 235, alpha)
            blue700 -> rgba(29, 78, 216, alpha)
            gray50 -> rgba(249, 250, 251, alpha)
            gray100 -> rgba(243, 244, 246, alpha)
            gray200 -> rgba(229, 231, 235, alpha)
            gray300 -> rgba(209, 213, 219, alpha)
            gray600 -> rgba(75, 85, 99, alpha)
            gray700 -> rgba(55, 65, 81, alpha)
            gray800 -> rgba(31, 41, 55, alpha)
            gray900 -> rgba(17, 24, 39, alpha)
            red100 -> rgba(254, 226, 226, alpha)
            red200 -> rgba(254, 202, 202, alpha)
            red500 -> rgba(239, 68, 68, alpha)
            red600 -> rgba(220, 38, 38, alpha)
            red700 -> rgba(185, 28, 28, alpha)
            else -> color
        }
    }
}
