package net.numa08.niconico_advertiser_list2.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.Cursor
import com.varabyte.kobweb.compose.css.UserSelect
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.components.icons.fa.FaDesktop
import com.varabyte.kobweb.silk.components.icons.fa.FaMoon
import com.varabyte.kobweb.silk.components.icons.fa.FaSun
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import com.varabyte.kobweb.silk.theme.colors.systemPreference
import kotlinx.browser.localStorage
import net.numa08.niconico_advertiser_list2.theme.Theme
import net.numa08.niconico_advertiser_list2.theme.current
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px

/**
 * カラーモード設定の種類
 */
enum class ColorModePreference {
    LIGHT,
    DARK,
    SYSTEM,
    ;

    companion object {
        private const val STORAGE_KEY = "color-mode-preference"

        fun loadFromLocalStorage(): ColorModePreference =
            try {
                val stored = localStorage.getItem(STORAGE_KEY)
                stored?.let { valueOf(it) } ?: SYSTEM
            } catch (_: Exception) {
                SYSTEM
            }

        fun saveToLocalStorage(preference: ColorModePreference) {
            localStorage.setItem(STORAGE_KEY, preference.name)
        }
    }
}

/**
 * カラーモード切り替えボタン
 */
@Composable
fun ColorModeToggle(modifier: Modifier = Modifier) {
    val currentColorState = ColorMode.currentState
    val systemPreference = ColorMode.systemPreference
    var preference by remember { mutableStateOf(ColorModePreference.loadFromLocalStorage()) }
    val theme = Theme.current

    // preferenceに基づいてColorModeを設定
    LaunchedEffect(preference, systemPreference) {
        val targetMode =
            when (preference) {
                ColorModePreference.LIGHT -> ColorMode.LIGHT
                ColorModePreference.DARK -> ColorMode.DARK
                ColorModePreference.SYSTEM -> systemPreference
            }
        if (currentColorState.value != targetMode) {
            currentColorState.value = targetMode
        }
    }

    Row(
        modifier =
            modifier
                .gap(0.25.cssRem)
                .padding(0.25.cssRem)
                .borderRadius(8.px)
                .backgroundColor(theme.surface)
                .border(1.px, org.jetbrains.compose.web.css.LineStyle.Solid, theme.border),
    ) {
        // ライトモードボタン
        ColorModeButton(
            icon = { FaSun(modifier = Modifier.color(theme.onSurface).fontSize(1.25.cssRem)) },
            isSelected = preference == ColorModePreference.LIGHT,
            onClick = {
                preference = ColorModePreference.LIGHT
                ColorModePreference.saveToLocalStorage(preference)
            },
            theme = theme,
        )

        // ダークモードボタン
        ColorModeButton(
            icon = { FaMoon(modifier = Modifier.color(theme.onSurface).fontSize(1.25.cssRem)) },
            isSelected = preference == ColorModePreference.DARK,
            onClick = {
                preference = ColorModePreference.DARK
                ColorModePreference.saveToLocalStorage(preference)
            },
            theme = theme,
        )

        // システム設定ボタン
        ColorModeButton(
            icon = { FaDesktop(modifier = Modifier.color(theme.onSurface).fontSize(1.25.cssRem)) },
            isSelected = preference == ColorModePreference.SYSTEM,
            onClick = {
                preference = ColorModePreference.SYSTEM
                ColorModePreference.saveToLocalStorage(preference)
            },
            theme = theme,
        )
    }
}

@Composable
private fun ColorModeButton(
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: Theme,
) {
    com.varabyte.kobweb.compose.foundation.layout.Box(
        modifier =
            Modifier
                .padding(topBottom = 0.5.cssRem, leftRight = 0.75.cssRem)
                .borderRadius(6.px)
                .backgroundColor(if (isSelected) theme.primaryContainer else theme.surface)
                .cursor(Cursor.Pointer)
                .userSelect(UserSelect.None)
                .onClick { onClick() },
    ) {
        icon()
    }
}
