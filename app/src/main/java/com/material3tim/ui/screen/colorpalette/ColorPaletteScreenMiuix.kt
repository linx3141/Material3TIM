package com.material3tim.ui.screen.colorpalette

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.ui.theme.PaletteStyle
import com.material3tim.ui.theme.ColorSpecVersion
import com.material3tim.ui.theme.effectiveFor
import com.material3tim.ui.theme.effectiveStyle
import com.material3tim.R
import com.material3tim.ui.component.bottombar.useNavigationRail
import com.material3tim.ui.component.miuix.ScaleDialog
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardSegment
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.theme.LocalEnableBlur
import com.material3tim.ui.theme.keyColorOptions
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ColorPaletteScreenMiuix(
    state: ColorPaletteUiState,
    actions: ColorPaletteScreenActions,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlurState = LocalEnableBlur.current
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/设备不支持 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlurState)
    // 分段玻璃 TabRow 位于页面 layerBackdrop 的录制内容内部：不能直接采样页面 backdrop，
    // 否则录制内容包含 TabRow 自身，形成 RenderNode 自采样环（重则 HWUI 原生栈溢出崩溃）。
    // 给 TabRow 单独一个只录制不透明底色的采样层（同一份兜底逻辑与表情/附件面板 Tab 相同）。
    // colorScheme 是 @Composable getter，必须在组合期取出后给 draw lambda 捕获。
    val surfaceColor = colorScheme.surface
    val tabBackdrop = rememberLayerBackdrop(
        graphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer(),
        onDraw = {
            drawRect(surfaceColor)
            drawContent()
        },
    )
    val listState = rememberLazyListState()
    val uiState = state.uiState
    val currentColorMode = state.currentColorMode
    val isDark = currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        // popupHost 使用 Scaffold 默认 MiuixPopupHost：GlassDropdownPreference 的玻璃下拉需要它。
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.settings_theme),
                    scrollBehavior = scrollBehavior,
                    backdrop = backdrop,
                    isContentScrolled = listState.canScrollBackward,
                    navigationIcon = {
                        // 顶栏按钮跟随顶栏风格：HyperTopBarGlassButton 自动选择玻璃圆钮 / 普通 IconButton。
                        // Back 图标在 RTL 下镜像，沿用迁移前行为。
                        HyperTopBarGlassButton(onClick = actions.onBack) {
                            val layoutDirection = LocalLayoutDirection.current
                            Icon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground
                            )
                        }
                    },
                )
            },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
        ) { innerPadding ->
            val showScaleDialog = rememberSaveable { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 采样层要早于 inset/padding：记录不透明底 + 页面内容（不含玻璃顶栏自身）。
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .layerBackdrop(backdrop)
                                .background(colorScheme.surface)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .padding(horizontal = 12.dp),
                    contentPadding = innerPadding,
                    overscrollEffect = null,
                ) {
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        ThemePreviewCardMiuix(
                            keyColor = uiState.keyColor,
                            isDark = isDark,
                            miuixMonet = uiState.miuixMonet,
                            followSystem = uiState.followSystem,
                            enableFloatingBottomBar = uiState.enableFloatingBottomBar,
                            enableFloatingBottomBarBlur = uiState.enableFloatingBottomBarBlur,
                            paletteStyle = state.currentPaletteStyle,
                            colorSpec = state.currentColorSpec,
                        )
                        Spacer(modifier = Modifier.height(72.dp))

                        val themeItems = listOf(
                            stringResource(id = R.string.settings_theme_mode_system),
                            stringResource(id = R.string.settings_theme_mode_light),
                            stringResource(id = R.string.settings_theme_mode_dark),
                        )
                        val selectedThemeTab = (if (uiState.themeMode >= 3) uiState.themeMode - 3 else uiState.themeMode).coerceIn(0, 2)
                        if (backdrop != null) {
                            // Phase 2e：backdrop 可用时用 miuix-glass 分段玻璃 TabRow（要求非空 backdrop）。
                            // 注意：TabRow 挂在页面 layerBackdrop 录制层内，必须采样独立的 tabBackdrop
                            //（只录制不透明底色），否则会采样到包含自身的录制内容 → RenderNode 递归崩溃。
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .layerBackdrop(tabBackdrop)
                                        .background(colorScheme.surface),
                                )
                                GlassSegmentedTabRow(
                                    tabs = themeItems,
                                    selectedIndex = selectedThemeTab,
                                    onSelect = { index ->
                                        actions.onSetThemeMode(index)
                                    },
                                    backdrop = tabBackdrop,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            // 无 backdrop（关闭模糊 / 设备不支持 runtime shader）→ 回退普通 TabRow。
                            TabRow(
                                tabs = themeItems,
                                selectedTabIndex = selectedThemeTab,
                                onTabSelected = { index ->
                                    actions.onSetThemeMode(index)
                                },
                            )
                        }

                        Card(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth(),
                        ) {
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_monet),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Wallpaper,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_monet),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.miuixMonet,
                                onCheckedChange = {
                                    actions.onSetMiuixMonet(it)
                                }
                            )

                            SwitchPreference(
                                title = stringResource(id = R.string.settings_follow_system),
                                summary = stringResource(id = R.string.settings_follow_system_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Wallpaper,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_follow_system),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = uiState.followSystem,
                                onCheckedChange = {
                                    actions.onSetFollowSystem(it)
                                }
                            )

                            AnimatedVisibility(
                                visible = uiState.miuixMonet
                            ) {
                                Column {
                                    val colorItems = listOf(
                                        stringResource(id = R.string.settings_key_color_default),
                                        stringResource(id = R.string.color_red),
                                        stringResource(id = R.string.color_pink),
                                        stringResource(id = R.string.color_purple),
                                        stringResource(id = R.string.color_deep_purple),
                                        stringResource(id = R.string.color_indigo),
                                        stringResource(id = R.string.color_blue),
                                        stringResource(id = R.string.color_cyan),
                                        stringResource(id = R.string.color_teal),
                                        stringResource(id = R.string.color_green),
                                        stringResource(id = R.string.color_yellow),
                                        stringResource(id = R.string.color_amber),
                                        stringResource(id = R.string.color_orange),
                                        stringResource(id = R.string.color_brown),
                                        stringResource(id = R.string.color_blue_grey),
                                        stringResource(id = R.string.color_sakura),
                                    )
                                    val colorValues = listOf(0) + keyColorOptions
                                    ColorPaletteDropdownPreference(
                                        title = stringResource(id = R.string.settings_key_color),
                                        items = colorItems,
                                        startAction = {
                                            Icon(
                                                Icons.Rounded.Colorize,
                                                modifier = Modifier.padding(end = 6.dp),
                                                contentDescription = stringResource(id = R.string.settings_key_color),
                                                tint = colorScheme.onBackground
                                            )
                                        },
                                        selectedIndex = if (uiState.followSystem) 0 else (colorValues.indexOf(uiState.keyColor).takeIf { it >= 0 } ?: 0),
                                        backdrop = backdrop,
                                        onSelectedIndexChange = { index ->
                                            actions.onSetKeyColor(colorValues[index])
                                        }
                                    )

                                    AnimatedVisibility(
                                        visible = uiState.keyColor != 0
                                    ) {
                                        Column {
                                            // SPEC 2026 只支持 CMF：此时风格下拉锁定为 CMF 且置灰不可点；
                                            // 切回 SPEC_2021/2025 后恢复用户之前选择的风格（持久化里没被改动）。
                                            val specLocksStyle =
                                                uiState.colorSpec == ColorSpecVersion.SPEC_2026.name
                                            val styles = if (specLocksStyle) {
                                                listOf(PaletteStyle.Cmf)
                                            } else {
                                                PaletteStyle.entries.filter { it != PaletteStyle.Cmf }
                                            }
                                            ColorPaletteDropdownPreference(
                                                title = stringResource(R.string.settings_color_style),
                                                summary = if (specLocksStyle) {
                                                    stringResource(R.string.settings_color_style_locked_2026)
                                                } else {
                                                    null
                                                },
                                                enabled = !specLocksStyle,
                                                startAction = {
                                                    Icon(
                                                        Icons.Rounded.Style,
                                                        modifier = Modifier.padding(end = 6.dp),
                                                        contentDescription = stringResource(id = R.string.settings_color_style),
                                                        tint = colorScheme.onBackground
                                                    )
                                                },
                                                items = styles.map {
                                                    if (it == PaletteStyle.Cmf) {
                                                        stringResource(R.string.settings_color_style_cmf)
                                                    } else {
                                                        it.name
                                                    }
                                                },
                                                selectedIndex = if (specLocksStyle) {
                                                    0
                                                } else {
                                                    styles.indexOfFirst { it.name == uiState.colorStyle }
                                                        .coerceAtLeast(0)
                                                },
                                                backdrop = backdrop,
                                                onSelectedIndexChange = { index ->
                                                    actions.onSetColorStyle(styles[index].name)
                                                }
                                            )

                                            val specs = ColorSpecVersion.entries
                                            ColorPaletteDropdownPreference(
                                                title = stringResource(R.string.settings_color_spec),
                                                startAction = {
                                                    Icon(
                                                        Icons.Rounded.DesignServices,
                                                        modifier = Modifier.padding(end = 6.dp),
                                                        contentDescription = stringResource(id = R.string.settings_color_spec),
                                                        tint = colorScheme.onBackground
                                                    )
                                                },
                                                items = specs.map { it.name },
                                                selectedIndex = specs.indexOfFirst { it.name == uiState.colorSpec }.coerceAtLeast(0),
                                                backdrop = backdrop,
                                                onSelectedIndexChange = { index ->
                                                    actions.onSetColorSpec(specs[index].name)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 「模糊（enableBlur）」偏全局、不属于底栏分组，继续留在原 Card 里；
                        // 该 Card 现在只剩这一个开关（API 33+ 才有），低版本整卡隐藏避免空卡片。
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Card(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth(),
                            ) {
                                SwitchPreference(
                                    title = stringResource(id = R.string.settings_enable_blur),
                                    summary = stringResource(id = R.string.settings_enable_blur_summary),
                                    startAction = {
                                        Icon(
                                            Icons.Rounded.BlurOn,
                                            modifier = Modifier.padding(end = 6.dp),
                                            contentDescription = stringResource(id = R.string.settings_enable_blur),
                                            tint = colorScheme.onBackground
                                        )
                                    },
                                    checked = uiState.enableBlur,
                                    onCheckedChange = {
                                        actions.onSetEnableBlur(it)
                                    }
                                )
                            }
                        }

                        // 「布局」分组：对齐 miuix example SettingsPage 的 Layout 分组
                        //（SmallTitle 分组标题 + 连续 squircle 分段卡片；排除 FloatingToolbar / FAB）。
                        // 条件行会改变首尾关系，所以先按可见行构建列表再算 isFirst/isLast。
                        Spacer(Modifier.height(12.dp))
                        SmallTitle(
                            text = stringResource(R.string.settings_layout_section),
                            // 列表本身已有 12dp 水平 padding，再补 16dp，使标题相对屏幕约 28dp（与 example 一致）。
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        )
                        val bottomBarRows = buildList {
                            // 顺序/条件对齐 example Layout 分组（手机布局，无宽屏 sidebar / FloatingToolbar / FAB）：
                            // 显示顶栏 → 顶栏风格；显示底栏 → 角标 → 导航栏模式(非悬浮) → 悬浮底栏
                            //   → 模糊(悬浮, API33+) → 悬浮底栏风格(悬浮) → 位置(悬浮且 Original)。
                            add(MiuixLayoutSettingRow.ShowTopAppBar)
                            if (uiState.showTopAppBar) {
                                add(MiuixLayoutSettingRow.TopAppBarStyle)
                            }
                            add(MiuixLayoutSettingRow.ShowNavigationBar)
                            if (uiState.showNavigationBar) {
                                add(MiuixLayoutSettingRow.NavigationBadge)
                                if (!uiState.enableFloatingBottomBar) {
                                    add(MiuixLayoutSettingRow.NavigationBarMode)
                                }
                                add(MiuixLayoutSettingRow.FloatingBottomBar)
                                if (uiState.enableFloatingBottomBar) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(MiuixLayoutSettingRow.FloatingBottomBarBlur)
                                    }
                                    add(MiuixLayoutSettingRow.FloatingNavigationBarStyle)
                                    if (uiState.floatingNavigationBarStyle.coerceIn(0, 2) == 0) {
                                        add(MiuixLayoutSettingRow.FloatingNavigationBarPosition)
                                    }
                                }
                            }
                        }
                        bottomBarRows.forEachIndexed { index, row ->
                            CardSegment(
                                isFirst = index == 0,
                                isLast = index == bottomBarRows.lastIndex,
                                // 列表已有水平 padding，这里不再额外缩进，保持与页面其它 Card 同宽。
                                outerHorizontalPadding = 0.dp,
                            ) {
                                when (row) {
                                    MiuixLayoutSettingRow.ShowTopAppBar -> SwitchPreference(
                                        title = stringResource(id = R.string.settings_show_top_app_bar),
                                        summary = stringResource(id = R.string.settings_show_top_app_bar_summary),
                                        checked = uiState.showTopAppBar,
                                        onCheckedChange = {
                                            actions.onSetShowTopAppBar(it)
                                        }
                                    )

                                    MiuixLayoutSettingRow.TopAppBarStyle -> ColorPaletteDropdownPreference(
                                        title = stringResource(R.string.settings_top_app_bar_style),
                                        items = listOf(
                                            stringResource(R.string.settings_floating_navigation_bar_style_original),
                                            stringResource(R.string.settings_floating_navigation_bar_style_glass),
                                        ),
                                        selectedIndex = uiState.topAppBarStyle.coerceIn(0, 1),
                                        startAction = null,
                                        backdrop = backdrop,
                                        onSelectedIndexChange = actions.onSetTopAppBarStyle,
                                    )

                                    MiuixLayoutSettingRow.ShowNavigationBar -> SwitchPreference(
                                        title = stringResource(id = R.string.settings_show_navigation_bar),
                                        summary = stringResource(id = R.string.settings_show_navigation_bar_summary),
                                        checked = uiState.showNavigationBar,
                                        onCheckedChange = {
                                            actions.onSetShowNavigationBar(it)
                                        }
                                    )

                                    MiuixLayoutSettingRow.NavigationBarMode -> ColorPaletteDropdownPreference(
                                        title = stringResource(R.string.settings_navigation_bar_mode),
                                        items = listOf(
                                            stringResource(R.string.settings_navigation_bar_mode_icon_text),
                                            stringResource(R.string.settings_navigation_bar_mode_icon_only),
                                            stringResource(R.string.settings_navigation_bar_mode_selected_label),
                                        ),
                                        selectedIndex = uiState.navigationBarMode.coerceIn(0, 2),
                                        startAction = null,
                                        backdrop = backdrop,
                                        onSelectedIndexChange = actions.onSetNavigationBarMode,
                                    )

                                    MiuixLayoutSettingRow.FloatingNavigationBarStyle -> ColorPaletteDropdownPreference(
                                        title = stringResource(R.string.settings_floating_navigation_bar_style),
                                        items = listOf(
                                            stringResource(R.string.settings_floating_navigation_bar_style_original),
                                            stringResource(R.string.settings_floating_navigation_bar_style_glass),
                                            stringResource(R.string.settings_floating_navigation_bar_style_ios_like),
                                        ),
                                        selectedIndex = uiState.floatingNavigationBarStyle.coerceIn(0, 2),
                                        startAction = null,
                                        backdrop = backdrop,
                                        onSelectedIndexChange = actions.onSetFloatingNavigationBarStyle,
                                    )

                                    MiuixLayoutSettingRow.FloatingNavigationBarPosition -> ColorPaletteDropdownPreference(
                                        title = stringResource(R.string.settings_floating_navigation_bar_position),
                                        items = listOf(
                                            stringResource(R.string.settings_floating_navigation_bar_position_center),
                                            stringResource(R.string.settings_floating_navigation_bar_position_start),
                                            stringResource(R.string.settings_floating_navigation_bar_position_end),
                                        ),
                                        selectedIndex = uiState.floatingNavigationBarPosition.coerceIn(0, 2),
                                        startAction = null,
                                        backdrop = backdrop,
                                        onSelectedIndexChange = actions.onSetFloatingNavigationBarPosition,
                                    )

                                    MiuixLayoutSettingRow.FloatingBottomBar -> SwitchPreference(
                                        title = stringResource(id = R.string.settings_floating_bottom_bar),
                                        summary = stringResource(id = R.string.settings_floating_bottom_bar_summary),
                                        checked = uiState.enableFloatingBottomBar,
                                        onCheckedChange = {
                                            actions.onSetEnableFloatingBottomBar(it)
                                        }
                                    )

                                    MiuixLayoutSettingRow.FloatingBottomBarBlur -> SwitchPreference(
                                        title = stringResource(id = R.string.settings_floating_bottom_bar_blur),
                                        summary = stringResource(id = R.string.settings_floating_bottom_bar_blur_summary),
                                        checked = uiState.enableFloatingBottomBarBlur,
                                        onCheckedChange = {
                                            actions.onSetEnableFloatingBottomBarBlur(it)
                                        }
                                    )

                                    MiuixLayoutSettingRow.NavigationBadge -> SwitchPreference(
                                        title = stringResource(id = R.string.settings_navigation_badge),
                                        summary = stringResource(id = R.string.settings_navigation_badge_summary),
                                        checked = uiState.enableNavigationBadge,
                                        onCheckedChange = {
                                            actions.onSetEnableNavigationBadge(it)
                                        }
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth(),
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                SwitchPreference(
                                    title = stringResource(id = R.string.settings_enable_predictive_back),
                                    summary = stringResource(id = R.string.settings_enable_predictive_back_summary),
                                    startAction = {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.MenuOpen,
                                            modifier = Modifier.padding(end = 6.dp),
                                            contentDescription = stringResource(id = R.string.settings_enable_predictive_back),
                                            tint = colorScheme.onBackground
                                        )
                                    },
                                    checked = uiState.enablePredictiveBack,
                                    onCheckedChange = {
                                        actions.onSetEnablePredictiveBack(it)
                                    }
                                )
                            }
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_enable_swipe_dismiss),
                                summary = stringResource(id = R.string.settings_enable_swipe_dismiss_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Swipe,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_enable_swipe_dismiss),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                checked = uiState.enableSwipeDismiss,
                                onCheckedChange = actions.onSetEnableSwipeDismiss,
                            )
                            ColorPaletteDropdownPreference(
                                title = stringResource(id = R.string.settings_pager_gesture_mode),
                                items = listOf(
                                    stringResource(id = R.string.settings_pager_gesture_native),
                                    stringResource(id = R.string.settings_pager_gesture_cross_axis),
                                    stringResource(id = R.string.settings_pager_gesture_ios_like),
                                ),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.ViewCarousel,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_pager_gesture_mode),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                selectedIndex = uiState.pagerInterceptionMode.coerceIn(0, 2),
                                backdrop = backdrop,
                                onSelectedIndexChange = actions.onSetPagerInterceptionMode,
                            )

                            var sliderValue by remember(uiState.pageScale) { mutableFloatStateOf(uiState.pageScale) }
                            ArrowPreference(
                                title = stringResource(id = R.string.settings_page_scale),
                                summary = stringResource(id = R.string.settings_page_scale_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.AspectRatio,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_page_scale),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                endActions = {
                                    Text(
                                        text = "${(sliderValue * 100).toInt()}%",
                                        color = colorScheme.onSurfaceVariantActions,
                                    )
                                },
                                onClick = { showScaleDialog.value = !showScaleDialog.value },
                                holdDownState = showScaleDialog.value,
                                bottomAction = {
                                    Slider(
                                        value = sliderValue,
                                        onValueChange = {
                                            sliderValue = it
                                        },
                                        onValueChangeFinished = {
                                            actions.onSetPageScale(sliderValue)
                                        },
                                        valueRange = 0.8f..1.1f,
                                        showKeyPoints = true,
                                        keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                                        magnetThreshold = 0.01f,
                                        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                    )
                                },
                            )
                            ScaleDialog(
                                show = showScaleDialog.value,
                                onDismissRequest = { showScaleDialog.value = false },
                                volumeState = { uiState.pageScale },
                                onVolumeChange = {
                                    actions.onSetPageScale(it)
                                }
                            )
                        }
                    }
                    item {
                        Spacer(
                            Modifier.height(
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                        WindowInsets.captionBar.asPaddingValues().calculateBottomPadding() +
                                        12.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCardMiuix(
    keyColor: Int,
    isDark: Boolean,
    miuixMonet: Boolean,
    followSystem: Boolean = false,
    enableFloatingBottomBar: Boolean = false,
    enableFloatingBottomBarBlur: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpecVersion = ColorSpecVersion.SPEC_2021,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
    val useRail = useNavigationRail(enableFloatingBottomBar)

    val seedColor = if (keyColor == 0) colorScheme.primary else Color(keyColor)
    val previewStyle = if (keyColor == 0) PaletteStyle.TonalSpot else paletteStyle
    val previewSpec = if (keyColor == 0) ColorSpecVersion.Default else colorSpec
    // 预览与真实主题一致：SPEC 2026 强制 CMF，CMF 强制 SPEC 2026。
    val effectiveStyle = previewSpec.effectiveStyle(previewStyle)
    val effectiveSpec = previewSpec.effectiveFor(effectiveStyle)
    val dynamicCs = if (followSystem) {
        com.material3tim.ui.theme.rememberM3TimColorScheme(
            seedColor = Color.Unspecified,
            isDark = isDark,
            isAmoled = false,
            paletteStyle = PaletteStyle.TonalSpot,
            colorSpec = ColorSpecVersion.SPEC_2025,
            followSystem = true,
        )
    } else {
        com.material3tim.ui.theme.rememberM3TimDynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            style = effectiveStyle,
            specVersion = effectiveSpec,
        )
    }
    val useDynamic = miuixMonet || followSystem

    val bgColor = if (useDynamic) dynamicCs.background else colorScheme.surface
    val textColor = if (useDynamic) dynamicCs.onSurface else colorScheme.onBackground
    val accentCardColor = when {
        useDynamic -> dynamicCs.secondaryContainer
        isDark -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val cardColor = if (useDynamic) dynamicCs.surfaceContainerHighest else colorScheme.surfaceVariant
    val navBarColor = if (useDynamic) dynamicCs.surfaceContainer else colorScheme.surface
    val iconColor = if (useDynamic) dynamicCs.primary else colorScheme.primary
    val navSelectedColor = colorScheme.onSurfaceContainer
    val navUnselectedColor = colorScheme.onSurfaceContainer.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
        ) {
            val content = @Composable {
                Column {
                    Row(
                        modifier = Modifier
                            .height(if (useRail) 36.dp else 48.dp)
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = if (useRail) 12.dp else 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontSize = 12.sp,
                            color = textColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentCardColor)
                    )

                    BoxWithConstraints(modifier = Modifier.weight(1f)) {
                        val smallCardHeight = 12.dp
                        val smallCardCount = when {
                            maxHeight >= 96.dp -> 2
                            maxHeight >= 72.dp -> 1
                            else -> 0
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(cardColor)
                            )
                            repeat(smallCardCount) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(smallCardHeight)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(cardColor)
                                )
                            }
                        }
                    }
                }
            }

            if (useRail) {
                Row {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(30.dp)
                            .background(navBarColor),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (it == 0) navSelectedColor else navUnselectedColor)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .background(textColor.copy(alpha = 0.1f))
                    )
                    Box(modifier = Modifier.weight(1f)) { content() }
                }
            } else {
                content()
            }

            if (!useRail && enableFloatingBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (enableFloatingBottomBarBlur) navBarColor.copy(alpha = 0.5f)
                                else navBarColor
                            )
                            .border(0.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (it == 0) iconColor else textColor)
                            )
                        }
                    }
                }
            } else if (!useRail) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(textColor.copy(alpha = 0.1f))
                    )
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth()
                            .background(navBarColor)
                            .padding(top = 2.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (it == 0) navSelectedColor else navUnselectedColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 主题页下拉设置项（Phase 2e）：
 * - backdrop 可用（模糊开启 + 设备支持 runtime shader）→ miuix-glass [GlassDropdownPreference]；
 * - 否则回退旧 [OverlayDropdownPreference]（Overlay 窗口弹层，兼容关闭模糊/低端机）。
 * 两者都保留同一份 items/selectedIndex/回调，弹层行为与选中逻辑不变。
 */
@Composable
private fun ColorPaletteDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    startAction: @Composable (() -> Unit)?,
    backdrop: Backdrop?,
    onSelectedIndexChange: (Int) -> Unit,
    enabled: Boolean = true,
    summary: String? = null,
) {
    if (backdrop != null && isRuntimeShaderSupported()) {
        GlassDropdownPreference(
            title = title,
            summary = summary,
            enabled = enabled,
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
            enabled = enabled,
            items = items,
            selectedIndex = selectedIndex,
            startAction = startAction,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}

/** 「布局」分组内的设置行（按顺序渲染；条件行会动态影响首尾圆角）。 */
private enum class MiuixLayoutSettingRow {
    /** 是否显示顶栏。 */
    ShowTopAppBar,
    /** 顶栏风格 Original / Glass（显示顶栏时显示）。 */
    TopAppBarStyle,
    /** 是否显示底栏/侧栏。 */
    ShowNavigationBar,
    /** 底栏角标（显示底栏时显示）。 */
    NavigationBadge,
    /** 标准底栏显示模式（显示底栏且非悬浮底栏时显示）。 */
    NavigationBarMode,
    /** 悬浮底栏开关（显示底栏时显示）。 */
    FloatingBottomBar,
    /** 悬浮底栏模糊（悬浮底栏开启且 API 33+）。 */
    FloatingBottomBarBlur,
    /** 悬浮底栏风格 Original / Glass / iOS-like（悬浮底栏开启时显示）。 */
    FloatingNavigationBarStyle,
    /** 悬浮底栏位置 居中/靠左/靠右（悬浮底栏且 Original 风格时显示）。 */
    FloatingNavigationBarPosition,
}
