package com.material3tim.ui.screen.chat


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.GlassMenuHost
import com.material3tim.ui.component.miuix.glass.GlassMenuState
import com.material3tim.ui.component.miuix.glass.LocalGlassMenu
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.HyperTopBarDropdownMenu
import com.material3tim.ui.component.miuix.hyper.HyperTopBarIconButton
import com.material3tim.ui.component.miuix.hyper.HyperTopBarMenuEntry
import com.material3tim.ui.theme.LocalTopAppBarStyle
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor

import com.material3tim.data.source.AppDataSources
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ChatItem
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.DataLineAvatar
import com.material3tim.ui.component.ScrollToTopOnChange
import com.material3tim.ui.component.SearchStatus
import com.material3tim.ui.component.miuix.SearchBarFake
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.material3tim.ui.ChatListJumpRequests
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.component.miuix.SearchBox
import com.material3tim.ui.component.miuix.SearchPager
import com.material3tim.ui.component.statustag.StatusTag
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.VolumeOff
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 消息页 Miuix 实现：对齐 KernelSU 超级用户列表的
 * HyperTopAppBar(SearchBarFake) + SearchPager + PullToRefresh + 独立会话卡片列表（Glass 采样 backdrop）。
 */
@Composable
fun ChatPagerMiuix(
    uiState: ChatUiState,
    actions: ChatActions,
    bottomInnerPadding: Dp,
) {
    val navigator = LocalNavigator.current
    // 群助手伪会话：点卡片进入群助手二级页，其它会话保持原行为。
    // 点击会话卡片：后台预取最近消息写入预览缓存，聊天页转场第一帧即可渲染（不阻塞导航）。
    // remember(actions)：未读角标/LiveEvents 等父级重组时不重建每行的点击 lambda。
    val onChatCardClick: (ChatItem) -> Unit = remember(actions) {
        { chat ->
            if (chat.id == GroupAssistantChatId) {
                navigator.push(Route.GroupAssistant)
            } else {
                if (AppDataSources.hasLiveMessages) {
                    // 点击即预取（共享 in-flight 请求）：聊天页进入时直接 await，转场结束即可显示气泡。
                    ChatMessagePreviewCache.prefetch(chat.id)
                }
                actions.onOpenChat(chat.id)
            }
        }
    }
    val searchStatus = uiState.searchStatus
    val enableBlur = LocalEnableBlur.current
    val density = LocalDensity.current

    val scrollBehavior = MiuixScrollBehavior()
    val dynamicTopPadding by remember {
        derivedStateOf { 12.dp * (1f - scrollBehavior.state.collapsedFraction) }
    }

    // Phase 2b：页面级 Glass 采样 backdrop（关闭模糊/无 runtime shader 时为 null，纯色回退）。
    val backdrop = rememberPageBackdrop(enableBlur)
    // 页面内容层长按菜单宿主状态：host 在 NavDisplay 子树内，返回手势优先关菜单。
    val glassMenu = remember { GlassMenuState() }
    val lazyListState = rememberLazyListState()
    // 滑动中把 LiveChats 推送暂存到 ViewModel，停止后再一次性应用：避免可见行在滚动手势帧里重组。
    // isScrollInProgress 在 effect 内用 snapshotFlow 读取，避免组合期直接读滚动状态；
    // 页面 dispose 时复位 false（保证离开首页后推送恢复即时应用）。
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .collect { actions.onListScrollStateChange(it) }
    }
    DisposableEffect(Unit) {
        onDispose { actions.onListScrollStateChange(false) }
    }

    // 底栏重复点「消息」：跳到下一个未读会话（排除免打扰，与角标口径一致），最后一个之后循环回第一个。
    val jumpRequests by ChatListJumpRequests.requests.collectAsStateWithLifecycle()
    var lastHandledJumpRequest by remember { mutableStateOf(0L) }
    var lastJumpedChatId by remember { mutableStateOf<String?>(null) }
    // P1：未读跳转统一走单次系统 animateScrollToItem（单段、精确、无停顿/两段弹簧；
    // 向上/回绕（如最后一个未读回到顶部）也不会出现两段接续）。
    LaunchedEffect(jumpRequests) {
        if (jumpRequests <= 0L || jumpRequests == lastHandledJumpRequest) return@LaunchedEffect
        // 先消费本次请求；搜索激活时忽略本次（Miuix 顶部胶囊入口已改为全局搜索页，正常不可达）。
        lastHandledJumpRequest = jumpRequests
        if (!searchStatus.isCollapsed()) return@LaunchedEffect
        val chats = uiState.chats
        val unreadChats = chats.filter { it.unreadCount > 0 && !it.isMuted }
        if (unreadChats.isEmpty()) return@LaunchedEffect
        val lastUnreadPosition = unreadChats.indexOfFirst { it.id == lastJumpedChatId }
        val nextChat = unreadChats[(lastUnreadPosition + 1) % unreadChats.size]
        val targetIndex = chats.indexOfFirst { it.id == nextChat.id }
        if (targetIndex >= 0) {
            lastJumpedChatId = nextChat.id
            // 单次系统动画精确滚到该行并停在列表顶部；新请求重启 LaunchedEffect 会取消上一次滚动。
            lazyListState.animateScrollToItem(targetIndex)
        }
    }

    // 右上角加号菜单：HyperOS4 顶栏圆形 Glass 按钮 → 按钮形状 morph 成玻璃菜单。
    // 按钮与菜单共用同一个锚点（rememberGlassPopupAnchor），预测性返回由 GlassTransformPopup 内部处理。
    var plusMenuExpanded by remember { mutableStateOf(false) }
    val plusMenuAnchor = rememberGlassPopupAnchor()
    // 顶栏风格=Original（或模糊不可用）时不用玻璃 morph 菜单，改回普通 miuix 下拉菜单。
    // 注意：这里不能用 isHyperTopBarGlassMode()——它读 LocalPageBackdrop，而页面 provider 在本行之后才提供
    // （会恒为 null，导致 Glass 风格下加号按钮/菜单错误回退成普通样式）。直接用已经算好的 backdrop 判断。
    val plusMenuGlassMode = LocalTopAppBarStyle.current == 1 &&
        backdrop != null && isRuntimeShaderSupported()
    // 页面 backdrop（供加号 morph 菜单采样；注意 provider 在本行之后，直接复用已建好的 backdrop）。
    val pageBackdrop = backdrop
    val plusMenuEntries = listOf(
        HyperTopBarMenuEntry(
            label = stringResource(R.string.home_plus_create_group),
            icon = MiuixIcons.Community,
            onClick = actions.onCreateGroup,
        ),
        HyperTopBarMenuEntry(
            label = stringResource(R.string.home_plus_add_friend),
            icon = MiuixIcons.AddCircle,
            onClick = actions.onAddFriend,
        ),
        HyperTopBarMenuEntry(
            label = stringResource(R.string.home_plus_scan),
            icon = MiuixIcons.Scan,
            onClick = actions.onScan,
        ),
    )

    CompositionLocalProvider(
        LocalPageBackdrop provides backdrop,
        LocalGlassMenu provides glassMenu,
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            HyperTopAppBar(
                title = stringResource(R.string.chats),
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                actions = {
                    if (plusMenuGlassMode) {
                        // 圆形 Glass 按钮：点击后由按钮形状 morph 出菜单（GlassTransformPopup）。
                        HyperTopBarIconButton(
                            onClick = { plusMenuExpanded = true },
                            contentDescription = stringResource(R.string.chat_new_chat),
                            icon = MiuixIcons.Add,
                            anchor = plusMenuAnchor,
                        )
                    } else {
                        // Original 风格：普通顶栏按钮 + 普通 miuix 下拉菜单（不 morph）。
                        OverlayIconDropdownMenu(
                            entries = listOf(
                                DropdownEntry(
                                    items = plusMenuEntries.map { entry ->
                                        DropdownItem(
                                            text = entry.label,
                                            icon = entry.icon?.let { icon ->
                                                { modifier ->
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = colorScheme.onSurface,
                                                        modifier = modifier.size(22.dp),
                                                    )
                                                }
                                            },
                                            onClick = entry.onClick,
                                        )
                                    },
                                ),
                            ),
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = stringResource(R.string.chat_new_chat),
                                tint = colorScheme.onSurface,
                            )
                        }
                    }
                },
                bottomContent = {
                            Box(
                                modifier = Modifier
                                    .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
                                    .onGloballyPositioned { coordinates ->
                                        with(density) {
                                            val newOffsetY = coordinates.positionInWindow().y.toDp()
                                            if (searchStatus.offsetY != newOffsetY) {
                                                actions.onSearchStatusChange(searchStatus.copy(offsetY = newOffsetY))
                                            }
                                        }
                                    }
                                    .then(
                                        if (searchStatus.isCollapsed()) {
                                            Modifier.pointerInput(Unit) {
                                                // 点顶部胶囊搜索框：进入全局搜索页（消息/联系人/群聊）。
                                                detectTapGestures {
                                                    navigator.push(Route.GlobalSearch)
                                                }
                                            }
                                        } else {
                                            Modifier
                                        },
                                    ),
                            ) {
                                SearchBarFake(
                                    label = stringResource(R.string.chat_search_hint),
                                    searchBarTopPadding = dynamicTopPadding,
                                )
                            }
                },
            )
        },
        popupHost = {
            searchStatus.SearchPager(
                onSearchStatusChange = actions.onSearchStatusChange,
                defaultResult = {
                    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                    if (uiState.pinnedChats.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .overScrollVertical(),
                        ) {
                            item {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.chat_search_pinned),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                )
                            }
                            items(uiState.pinnedChats, key = { it.id }, contentType = { "pinned-chat" }) { chat ->
                                ChatCardMiuix(
                                    chat = chat,
                                    actions = actions,
                                    onClick = {
                                        actions.onSearchStatusChange(
                                            searchStatus.copy(
                                                searchText = "",
                                                current = SearchStatus.Status.COLLAPSING,
                                            )
                                        )
                                        onChatCardClick(chat)
                                    },
                                )
                            }
                            item {
                                Spacer(Modifier.height(maxOf(bottomInnerPadding, imeBottomPadding)))
                            }
                        }
                    }
                },
                searchBarTopPadding = dynamicTopPadding,
            ) {
                val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollVertical(),
                ) {
                    item {
                        Spacer(Modifier.height(6.dp))
                    }
                    if (uiState.chats.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.chat_search_empty),
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                    items(uiState.chats, key = { it.id }, contentType = { "chat" }) { chat ->
                        ChatCardMiuix(
                            chat = chat,
                            actions = actions,
                            onClick = {
                                actions.onSearchStatusChange(
                                    searchStatus.copy(
                                        searchText = "",
                                        current = SearchStatus.Status.COLLAPSING,
                                    )
                                )
                                onChatCardClick(chat)
                            },
                        )
                    }
                    item {
                        Spacer(Modifier.height(maxOf(bottomInnerPadding, imeBottomPadding)))
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        searchStatus.SearchBox {
            val refreshTick = remember { mutableIntStateOf(0) }
            val latestChats = rememberUpdatedState(uiState.chats)
            val latestRefreshing = rememberUpdatedState(uiState.isRefreshing)
            ScrollToTopOnChange(
                lazyListState,
                refreshTick.intValue,
                isBusy = { latestRefreshing.value },
            ) { latestChats.value }

            val pullToRefreshState = rememberPullToRefreshState()
            val refreshTexts = listOf(
                stringResource(R.string.chat_refresh_pulling),
                stringResource(R.string.chat_refresh_release),
                stringResource(R.string.chat_refresh_refreshing),
                stringResource(R.string.chat_refresh_complete),
            )

            PullToRefresh(
                isRefreshing = uiState.isRefreshing,
                pullToRefreshState = pullToRefreshState,
                onRefresh = {
                    actions.onRefresh()
                    refreshTick.intValue++
                },
                refreshTexts = refreshTexts,
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 6.dp,
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                ),
            ) {
                Box {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
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
                            ),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + 6.dp,
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            end = innerPadding.calculateEndPadding(layoutDirection),
                        ),
                        overscrollEffect = null,
                    ) {
                        // 会话列表：每条会话一张独立卡片（间隔由 ChatCardMiuix 的 12dp 下边距提供；
                        // 与置顶区/搜索页一致，不再使用 HyperOS4 分段卡片）。
                        items(uiState.chats, key = { it.id }, contentType = { "chat" }) { chat ->
                            ChatCardMiuix(
                                chat = chat,
                                actions = actions,
                                onClick = { onChatCardClick(chat) },
                            )
                        }
                        item {
                            Spacer(Modifier.height(bottomInnerPadding + 6.dp))
                        }
                    }
                }
            }
        }
    }
        // 加号菜单（仅 Glass 风格）：常驻组合（show 控制显隐），从加号按钮形状 morph 展开；
        // Original 风格使用上面的 OverlayIconDropdownMenu，不需要这里。
        if (plusMenuGlassMode) {
            HyperTopBarDropdownMenu(
                show = plusMenuExpanded,
                onDismissRequest = { plusMenuExpanded = false },
                anchor = plusMenuAnchor,
                entries = plusMenuEntries,
                anchorContent = {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                },
                backdrop = pageBackdrop,
            )
        }
        // 页面内容层长按菜单宿主：在 NavDisplay 子树内注册 GlassPopup 的返回处理器，
        // 返回手势会先关闭菜单（预测性返回跟手），菜单关闭后再按返回才退出页面。
        GlassMenuHost(glassMenu)
    }
    }
}

/** 群助手固定图标（Miuix 风格，不使用普通头像/在线点）。 */
@Composable
private fun GroupAssistantAvatarMiuix(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            tint = colorScheme.onPrimaryContainer,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** 单条会话卡片（首页/搜索页）：48dp 头像 + 名称/摘要 + 时间/未读/免打扰/置顶标签；置顶卡片带主题色竖条。 */
@Composable
internal fun ChatCardMiuix(
    chat: ChatItem,
    actions: ChatActions,
    onClick: () -> Unit,
    scrolling: Boolean = false,
) {
    ChatCardActionMenuHost(chat = chat, actions = actions) {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            onClick = onClick,
            showIndication = true,
            insideMargin = PaddingValues(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            ChatRowContentMiuix(chat, scrolling = scrolling)
        }
    }
}

/** 分段卡片用会话行：无 Card 容器（背景/圆角由 CardSegment 提供），保留长按菜单与点击。
 *  首页已改回 [ChatCardMiuix] 独立卡片；本函数仍被群助手页等分段卡片列表复用。 */
@Composable
internal fun ChatRowItemMiuix(
    chat: ChatItem,
    actions: ChatActions,
    onClick: () -> Unit,
    scrolling: Boolean = false,
) {
    ChatCardActionMenuHost(chat = chat, actions = actions) {
        ChatRowContentMiuix(
            chat = chat,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ChatRowContentMiuix(
    chat: ChatItem,
    modifier: Modifier = Modifier,
    scrolling: Boolean = false,
) {
    val pinned = chat.isPinned
    // 摘要前可能带提醒标签（如 [有人@我]）：标签用 Miuix 错误红色，其余保持次级文字色。
    val summary = chatSummaryAnnotated(chat, tagColor = colorScheme.error)
    // 说明：不要用 container 色直接做卡片底色再配 on 色（Miuix 默认调色板对比度不适合）。
    // 置顶只用主题色竖条 + 置顶标签表达，文字始终用 onSurface / onSurfaceVariantSummary。
    val titleColor = colorScheme.onSurface
    val secondaryColor = colorScheme.onSurfaceVariantSummary
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            if (pinned) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(width = 4.dp, height = 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primary),
                )
            }
            if (chat.id == GroupAssistantChatId) {
                GroupAssistantAvatarMiuix(modifier = Modifier.padding(end = 10.dp))
            } else if (chat.dataLineKind != 0) {
                DataLineAvatar(
                    kind = chat.dataLineKind,
                    modifier = Modifier.padding(end = 10.dp),
                )
            } else {
                ChatAvatar(
                    name = chat.name,
                    seed = chat.avatarSeed,
                    isOnline = chat.isOnline,
                    isGroup = chat.isGroup,
                    avatarPath = chat.avatarPath,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.name,
                    // 滚动中暂停跑马灯，减少滚动期间的持续 layout/动画开销。
                    modifier = if (scrolling) Modifier else Modifier.basicMarquee(),
                    fontWeight = FontWeight(550),
                    color = titleColor,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = summary,
                    // 滚动中暂停跑马灯，减少滚动期间的持续 layout/动画开销。
                    modifier = if (scrolling) Modifier else Modifier.basicMarquee(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = secondaryColor,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Column(
                modifier = Modifier.padding(start = 12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = chat.timeLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight(550),
                    color = secondaryColor,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (chat.isPinned) {
                        StatusTag(
                            label = stringResource(R.string.chat_pinned),
                            backgroundColor = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer,
                        )
                    }
                    if (chat.isMuted) {
                        Icon(
                            imageVector = MiuixIcons.VolumeOff,
                            tint = secondaryColor,
                            contentDescription = stringResource(R.string.chat_muted),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (chat.unreadCount > 0) {
                        // 免打扰会话的未读徽标用更淡的 surfaceVariant 角色（Miuix 等价配色），
                        // 文字用其 surface 上的次级文字色 onSurfaceVariantSummary，与正常未读（primary）区分。
                        val mutedUnread = chat.isMuted
                        Badge(
                            containerColor = if (mutedUnread) {
                                colorScheme.surfaceVariant
                            } else {
                                colorScheme.primary
                            },
                            contentColor = if (mutedUnread) {
                                colorScheme.onSurfaceVariantSummary
                            } else {
                                colorScheme.onPrimary
                            },
                        ) {
                            Text(text = unreadBadgeText(chat.unreadCount))
                        }
                    }
                    }
                }
            }
}
