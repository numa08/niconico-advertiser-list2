package net.numa08.niconico_advertiser_list2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.varabyte.kobweb.compose.css.ScrollBehavior
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxHeight
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.flexGrow
import com.varabyte.kobweb.compose.ui.modifiers.minHeight
import com.varabyte.kobweb.compose.ui.modifiers.scrollBehavior
import com.varabyte.kobweb.core.App
import com.varabyte.kobweb.silk.SilkApp
import com.varabyte.kobweb.silk.components.layout.Surface
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.init.registerStyleBase
import com.varabyte.kobweb.silk.style.common.SmoothColorStyle
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import com.varabyte.kobweb.silk.theme.colors.loadFromLocalStorage
import com.varabyte.kobweb.silk.theme.colors.saveToLocalStorage
import com.varabyte.kobweb.silk.theme.colors.systemPreference
import kotlinx.browser.window
import net.numa08.niconico_advertiser_list2.components.Footer
import net.numa08.niconico_advertiser_list2.components.Header
import net.numa08.niconico_advertiser_list2.theme.AppTheme
import net.numa08.niconico_advertiser_list2.util.GoogleAnalytics
import org.jetbrains.compose.web.css.vh

private const val COLOR_MODE_KEY = "niconico_advertiser_list2:colorMode"

@InitSilk
fun initColorMode(ctx: InitSilkContext) {
    ctx.config.initialColorMode = ColorMode.loadFromLocalStorage(COLOR_MODE_KEY) ?: ColorMode.systemPreference
}

@InitSilk
fun initStyles(ctx: InitSilkContext) {
    ctx.stylesheet.apply {
        registerStyleBase("html, body") { Modifier.fillMaxHeight() }
        registerStyleBase("body") { Modifier.scrollBehavior(ScrollBehavior.Smooth) }
    }
}

@App
@Composable
fun AppEntry(content: @Composable () -> Unit) {
    SilkApp {
        val colorMode = ColorMode.current
        LaunchedEffect(colorMode) {
            colorMode.saveToLocalStorage(COLOR_MODE_KEY)
        }

        // ページビュー追跡
        LaunchedEffect(Unit) {
            GoogleAnalytics.trackPageView(
                pagePath = window.location.pathname,
                pageTitle = window.document.title,
            )
        }

        AppTheme {
            Surface(SmoothColorStyle.toModifier().minHeight(100.vh)) {
                Column(modifier = Modifier.fillMaxWidth().minHeight(100.vh)) {
                    // ヘッダー
                    Header()

                    // メインコンテンツ
                    Box(modifier = Modifier.fillMaxWidth().flexGrow(1)) {
                        content()
                    }

                    // フッター
                    Footer()
                }
            }
        }
    }
}
