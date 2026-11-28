package com.material3tim.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ChatItem
import com.material3tim.ui.component.miuix.glass.GlassMenuHost
import com.material3tim.ui.component.miuix.glass.GlassMenuState
import com.material3tim.ui.component.miuix.glass.LocalGlassMenu
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 群助手页（Miuix）：标题「群助手」+ 返回；群列表复用消息列表的会话行样式
 * （[ChatRowItemMiuix]：群头像/群名/未读/摘要/时间），点群进入对应群聊。
 *
 * Phase 2e：HyperOS4 化 —— 玻璃/纯色自适应顶栏 + 页面级 backdrop + 分段卡片（CardSegment）。
 */
@Composable
internal fun GroupAssistantMiuix(
    groups: List<ChatItem>,
    loading: Boolean,
    failed: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenGroup: (ChatItem) -> Unit,
    title: String? = null,
) {
    val rowActions = remember {
        ChatActions(
            onRefresh = {},
            onSearchTextChange = {},
            onSearchStatusChange = { _ -> },
            onClearSearch = {},
            onOpenChat = { _ -> },
        )
    }
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/设备不支持 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)
    // 页面内容层长按菜单宿主状态：群列表复用会话卡片长按菜单，host 在页面内以取得返回优先级。
    val glassMenu = remember { GlassMenuState() }

    CompositionLocalProvider(
        LocalPageBackdrop provides backdrop,
        LocalGlassMenu provides glassMenu,
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = title ?: stringResource(R.string.group_assistant_title),
                    scrollBehavior = scrollBehavior,
                    backdrop = backdrop,
                    isContentScrolled = listState.canScrollBackward,
                    navigationIcon = {
                        // 顶栏按钮跟随顶栏风格：HyperTopBarGlassButton 自动选择玻璃圆钮 / 普通 IconButton。
                        HyperTopBarGlassButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.chat_detail_back),
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            // 内容容器只保留 start/end/bottom inset：top 交给列表 contentPadding，
            // 让列表视口延伸到 Glass 顶栏下方（否则玻璃只能采到 surface 色）。
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
                    )
                    .padding(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                        bottom = innerPadding.calculateBottomPadding(),
                    ),
            ) {
                when {
                    loading && groups.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        InfiniteProgressIndicator()
                    }
                    failed && groups.isEmpty() -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.group_files_load_failed),
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                        TextButton(
                            text = stringResource(R.string.group_files_retry),
                            onClick = onRetry,
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                    groups.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.group_assistant_empty),
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .scrollEndHaptic()
                            .overScrollVertical()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            // 首帧不被顶栏遮住；滚动时内容从顶栏下方穿过。
                            top = innerPadding.calculateTopPadding() + 6.dp,
                            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        ),
                        overscrollEffect = null,
                    ) {
                        // HyperOS4 分组卡片：群聊行合并为 squircle 分段卡片（首/尾圆角自动处理）。
                        groupedCardItems(
                            keyPrefix = "groupAssistant",
                            outerTopPadding = 6.dp,
                            outerBottomPadding = 6.dp,
                            items = groups.map { group ->
                                CardItem(group.id) {
                                    ChatRowItemMiuix(
                                        chat = group,
                                        actions = rowActions,
                                        onClick = { onOpenGroup(group) },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    // 长按菜单宿主：页面内容层最后渲染（返回优先级在页面处理器之后）。
    GlassMenuHost(glassMenu)
    }
    }
}
