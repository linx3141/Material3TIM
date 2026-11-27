package com.material3tim.ui.screen.settings

import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 设置页（Miuix 风格）。
 * 只保留与 UI 有关的设置：界面风格、主题、关于。
 */
@Composable
fun SettingPagerMiuix(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
) {
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    // Phase 2a：页面级 Glass 采样 backdrop；关闭模糊/设备不支持时为 null → 玻璃控件自动纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.settings),
                    isContentScrolled = listState.canScrollBackward,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        // 顶栏按钮跟随顶栏风格：HyperTopBarGlassButton 自动选择玻璃圆钮 / 普通 IconButton。
                        HyperTopBarGlassButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
            // popupHost 使用 Scaffold 默认 MiuixPopupHost：GlassDropdownPreference 的玻璃下拉需要它。
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    // 采样层要早于 inset/padding：记录不透明底 + 列表内容（不含玻璃顶栏自身）。
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .layerBackdrop(backdrop)
                                .background(colorScheme.surface)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                // HyperOS4 分组卡片（CardSegment/groupedCardItems）：圆角分段 + 黑缝防透出。
                groupedCardItems(
                    keyPrefix = "settingsAppearance",
                    outerTopPadding = 12.dp,
                    outerBottomPadding = 6.dp,
                    // 列表本身已有 12dp 水平 padding；这里再叠加默认 12dp 会变成 24dp 边距（偏大）。
                    outerHorizontalPadding = 0.dp,
                    items = listOf(
                        CardItem("uiMode") {
                            // Phase 2f：backdrop 可用时用 miuix-glass 玻璃下拉，否则回退旧 Overlay 弹层。
                            MiuixDropdownPreference(
                                title = stringResource(id = R.string.settings_ui_mode),
                                summary = stringResource(id = R.string.settings_ui_mode_summary),
                                items = UiMode.entries.map { it.name },
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Filled.DisplaySettings,
                                        contentDescription = stringResource(id = R.string.settings_ui_mode),
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                selectedIndex = if (uiState.uiMode == UiMode.Material.value) 1 else 0,
                                backdrop = backdrop,
                                onSelectedIndexChange = actions.onSetUiModeIndex,
                            )
                        },
                        CardItem("theme") {
                            ArrowPreference(
                                title = stringResource(id = R.string.settings_theme),
                                summary = stringResource(id = R.string.settings_theme_summary),
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Filled.Palette,
                                        contentDescription = stringResource(id = R.string.settings_theme),
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = actions.onOpenTheme,
                            )
                        },
                        CardItem("telegramStickers") {
                            ArrowPreference(
                                title = stringResource(R.string.tg_settings),
                                summary = stringResource(R.string.tg_settings_summary),
                                onClick = actions.onSyncTelegram,
                            )
                        },
                        CardItem("timSettings") {
                            ArrowPreference(
                                title = stringResource(id = R.string.settings_open_tim_settings),
                                summary = stringResource(
                                    id = R.string.settings_open_tim_settings_summary,
                                ),
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = stringResource(
                                            id = R.string.settings_open_tim_settings,
                                        ),
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = actions.onOpenTimSettings,
                            )
                        },
                        CardItem("returnOriginalTim") {
                            ArrowPreference(
                                title = stringResource(id = R.string.settings_return_original_tim),
                                summary = stringResource(
                                    id = R.string.settings_return_original_tim_summary,
                                ),
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Filled.RestartAlt,
                                        contentDescription = stringResource(
                                            id = R.string.settings_return_original_tim,
                                        ),
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = actions.onReturnToOriginalTim,
                            )
                        },
                        CardItem("about") {
                            ArrowPreference(
                                title = stringResource(id = R.string.about),
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = stringResource(id = R.string.about),
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = actions.onOpenAbout,
                            )
                        },
                    ),
                )
                item(key = "note") {
                    Text(
                        text = stringResource(R.string.settings_coming_soon),
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                    )
                }
                item(key = "bottom_padding") {
                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                    24.dp
                        )
                    )
                }
            }
        }
    }
}

/**
 * 设置页下拉设置项（Phase 2f）：
 * - backdrop 可用（模糊开启 + 设备支持 runtime shader）→ miuix-glass [GlassDropdownPreference]；
 * - 否则回退旧 [OverlayDropdownPreference]（Overlay 窗口弹层，兼容关闭模糊/低端机）。
 * 两者共用同一份 items/selectedIndex/回调，弹层行为与选中逻辑不变。
 */
@Composable
private fun MiuixDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    backdrop: Backdrop?,
    startAction: @Composable (() -> Unit)? = null,
    summary: String? = null,
    onSelectedIndexChange: (Int) -> Unit,
) {
    if (backdrop != null && isRuntimeShaderSupported()) {
        GlassDropdownPreference(
            title = title,
            summary = summary,
            items = items,
            selectedIndex = selectedIndex,
            startAction = startAction,
            backdrop = backdrop,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    } else {
        OverlayDropdownPreference(
            title = title,
            summary = summary,
            items = items,
            selectedIndex = selectedIndex,
            startAction = startAction,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}
