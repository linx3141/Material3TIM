package com.material3tim.ui.screen.chat

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.material3tim.R
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.ContactItem
import com.material3tim.data.model.GlobalSearchMessage
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.DataLineAvatar
import com.material3tim.ui.screen.search.GlobalSearchResultsMaterial
import com.material3tim.ui.screen.search.GlobalSearchUiState
import com.material3tim.ui.component.ScrollToTopOnChange
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.OffsetAnchoredExpressiveMenu
import com.material3tim.ui.component.material.SearchAppBar
import com.material3tim.ui.ChatListJumpRequests
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SnackBarHost
import com.material3tim.ui.component.material.SegmentedItem
import com.material3tim.ui.component.material.expressiveTopAppBarColors
import com.material3tim.ui.component.material.trackPressPosition
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.statustag.StatusTag

/**
 * 消息页 Material 3 Expressive 实现：布局对齐 KernelSU 超级用户列表
 * （ExpressiveScaffold + 顶栏/胶囊搜索栏 + PullToRefreshBox + SegmentedListItem 行）。
 */
@Composable
fun ChatPagerMaterial(
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    // 滑动中把 LiveChats 推送暂存到 ViewModel，停止后再一次性应用：避免可见行在滚动手势帧里重组。
    // isScrollInProgress 在 effect 内用 snapshotFlow 读取，避免组合期直接读滚动状态；
    // 页面 dispose 时复位 false（保证离开首页后推送恢复即时应用）。
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { actions.onListScrollStateChange(it) }
    }
    DisposableEffect(Unit) {
        onDispose { actions.onListScrollStateChange(false) }
    }
    val refreshTick = remember { mutableIntStateOf(0) }
    val pullToRefreshState = rememberPullToRefreshState()

    // 底栏重复点「消息」：跳到下一个未读会话（排除免打扰，与角标口径一致），最后一个之后循环回第一个。
    val jumpRequests by ChatListJumpRequests.requests.collectAsStateWithLifecycle()
    var lastHandledJumpRequest by remember { mutableStateOf(0L) }
    var lastJumpedChatId by remember { mutableStateOf<String?>(null) }
    // P1：未读跳转统一走单次系统 animateScrollToItem（单段、精确、无停顿/两段弹簧；
    // 向上/回绕（如最后一个未读回到顶部）也不会出现两段接续）。
    LaunchedEffect(jumpRequests) {
        if (jumpRequests <= 0L || jumpRequests == lastHandledJumpRequest) return@LaunchedEffect
        lastHandledJumpRequest = jumpRequests
        val chats = uiState.chats
        val unreadChats = chats.filter { it.unreadCount > 0 && !it.isMuted }
        if (unreadChats.isEmpty()) return@LaunchedEffect
        val lastUnreadPosition = unreadChats.indexOfFirst { it.id == lastJumpedChatId }
        val nextChat = unreadChats[(lastUnreadPosition + 1) % unreadChats.size]
        // 两段分组后 LazyColumn 多了标题 item：按「置顶标题/置顶行/全部标题/普通行」重算实际下标。
        val targetItemIndex = groupedChatItemIndex(chats, nextChat.id)
        if (targetItemIndex >= 0) {
            lastJumpedChatId = nextChat.id
            // 单次系统动画精确滚到该行并停在列表顶部；新请求重启 LaunchedEffect 会取消上一次滚动。
            listState.animateScrollToItem(targetItemIndex)
        }
    }

    // 右上角加号菜单（M3E Expressive 下拉，与设置页「色彩风格」同一个菜单组件）。
    var plusMenuExpanded by remember { mutableStateOf(false) }
    var plusMenuAnchor by remember { mutableStateOf(IntOffset.Zero) }

    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    // 首页搜索栏：与全局搜索页共用数据源（AppDataSources.searchSource?.searchAll），就地展开显示
    // 消息/联系人/群分区结果，不跳转独立 GlobalSearch 页面。
    var globalSearchKeyword by remember { mutableStateOf("") }
    var globalSearchRetryTick by remember { mutableIntStateOf(0) }
    var globalSearchState by remember {
        mutableStateOf<GlobalSearchUiState>(GlobalSearchUiState.Idle)
    }
    LaunchedEffect(globalSearchKeyword, globalSearchRetryTick) {
        val query = globalSearchKeyword.trim()
        if (query.isEmpty()) {
            globalSearchState = GlobalSearchUiState.Idle
            return@LaunchedEffect
        }
        // 防抖 300ms（与全局搜索页一致）：连续输入只搜最后一次。
        delay(300L)
        globalSearchState = GlobalSearchUiState.Loading
        val source = AppDataSources.searchSource
        if (source == null) {
            globalSearchState = GlobalSearchUiState.HostOnly
            return@LaunchedEffect
        }
        val raw = withContext(Dispatchers.IO) {
            runCatching { source.searchAll(query, 50) }.getOrNull()
        }
        globalSearchState = when {
            raw == null -> GlobalSearchUiState.Failed
            raw.messages.isEmpty() && raw.contacts.isEmpty() && raw.groups.isEmpty() ->
                GlobalSearchUiState.Empty
            else -> GlobalSearchUiState.Results(raw)
        }
    }
    val homeKeyboardController = LocalSoftwareKeyboardController.current
    val onOpenSearchMessage: (GlobalSearchMessage) -> Unit = { message ->
        homeKeyboardController?.hide()
        navigator.push(
            Route.ChatDetail(
                chatId = message.chatId,
                jumpToSeq = message.msgSeq,
                jumpToMsgId = message.msgId,
            ),
        )
    }
    val onOpenSearchContact: (ContactItem) -> Unit = { contact ->
        homeKeyboardController?.hide()
        navigator.push(Route.ContactDetail(contactId = contact.id))
    }

    ExpressiveScaffold(
        topBar = {
            // 还原为联系人页同款 SearchAppBar：LargeFlexible 标题 + 胶囊搜索栏 + 展开层。
            SearchAppBar(
                snackbarHostState = snackbarHostState,
                title = { Text(stringResource(R.string.chats)) },
                searchText = globalSearchKeyword,
                onSearchTextChange = { globalSearchKeyword = it },
                onClearClick = { globalSearchKeyword = "" },
                actions = {
                    Box(
                        modifier = Modifier.trackPressPosition { plusMenuAnchor = it.round() },
                    ) {
                        IconButton(onClick = { plusMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.chat_new_chat),
                            )
                        }
                        OffsetAnchoredExpressiveMenu(
                            expanded = plusMenuExpanded,
                            onDismissRequest = { plusMenuExpanded = false },
                            anchorOffset = plusMenuAnchor,
                        ) {
                            HomePlusMenu.entries.forEachIndexed { index, entry ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(entry.labelRes)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = entry.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                        )
                                    },
                                    onClick = {
                                        plusMenuExpanded = false
                                        when (entry) {
                                            HomePlusMenu.CreateGroup -> actions.onCreateGroup()
                                            HomePlusMenu.AddFriendGroup -> actions.onAddFriend()
                                            HomePlusMenu.Scan -> actions.onScan()
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                defaultContent = { _, _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.global_search_idle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                alwaysCollapsedTopBar = true,
                searchContent = { bottomPadding, closeSearch ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp + bottomPadding),
                    ) {
                        when (val searchState = globalSearchState) {
                            GlobalSearchUiState.Idle -> Text(
                                text = stringResource(R.string.global_search_idle),
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            GlobalSearchUiState.HostOnly -> Text(
                                text = stringResource(R.string.global_search_host_only),
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            GlobalSearchUiState.Empty -> Text(
                                text = stringResource(R.string.global_search_empty),
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            GlobalSearchUiState.Loading -> CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            GlobalSearchUiState.Failed -> Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.global_search_failed),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(
                                    onClick = { globalSearchRetryTick += 1 },
                                ) {
                                    Text(stringResource(R.string.global_search_retry))
                                }
                            }
                            is GlobalSearchUiState.Results -> GlobalSearchResultsMaterial(
                                result = searchState.result,
                                onOpenMessage = { message ->
                                    closeSearch()
                                    onOpenSearchMessage(message)
                                },
                                onOpenContact = { contact ->
                                    closeSearch()
                                    onOpenSearchContact(contact)
                                },
                                onOpenGroup = { group ->
                                    closeSearch()
                                    homeKeyboardController?.hide()
                                    navigator.push(Route.ChatDetail(chatId = group.id))
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackBarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                actions.onRefresh()
                refreshTick.intValue++
            },
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = uiState.isRefreshing,
                    state = pullToRefreshState,
                )
            },
        ) {
            val latestChats = rememberUpdatedState(uiState.chats)
            val latestRefreshing = rememberUpdatedState(uiState.isRefreshing)
            ScrollToTopOnChange(
                listState,
                refreshTick.intValue,
                isBusy = { latestRefreshing.value },
            ) { latestChats.value }

            // 置顶/普通分组只在 chats 变化时重算，避免未读角标等高频状态更新时重复 filter。
            val pinnedChats = remember(uiState.chats) { uiState.chats.filter { it.isPinned } }
            val normalChats = remember(uiState.chats) { uiState.chats.filterNot { it.isPinned } }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 0.dp,
                    bottom = 16.dp + bottomInnerPadding,
                ),
            ) {
                // 置顶会话 / 全部会话两段：标题样式与 SegmentedColumn(title=...) 一致；
                // 行仍用 LazyColumn items + SegmentedItem（保持懒加载/稳定 key/分组形状）。
                if (pinnedChats.isNotEmpty()) {
                    item(key = "chat_group_pinned_title") {
                        ChatGroupTitleMaterial(stringResource(R.string.chat_group_pinned))
                    }
                    itemsIndexed(
                        pinnedChats,
                        key = { _, chat -> chat.id },
                        contentType = { _, _ -> "chat" },
                    ) { index, chat ->
                        SegmentedItem(index = index, count = pinnedChats.size) {
                            ChatListRowMaterial(
                                chat = chat,
                                actions = actions,
                                scrolling = listState.isScrollInProgress,
                            ) {
                                onChatCardClick(chat)
                            }
                        }
                    }
                }
                if (normalChats.isNotEmpty()) {
                    item(key = "chat_group_all_title") {
                        ChatGroupTitleMaterial(
                            title = stringResource(R.string.chat_group_all),
                            topPadding = 8.dp,
                        )
                    }
                    itemsIndexed(
                        normalChats,
                        key = { _, chat -> chat.id },
                        contentType = { _, _ -> "chat" },
                    ) { index, chat ->
                        SegmentedItem(index = index, count = normalChats.size) {
                            ChatListRowMaterial(
                                chat = chat,
                                actions = actions,
                                scrolling = listState.isScrollInProgress,
                            ) {
                                onChatCardClick(chat)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 分组标题：与 SegmentedColumn(title=...) 同款样式（titleSmallEmphasized / primary / start16 + bottom8）。 */
@Composable
private fun ChatGroupTitleMaterial(title: String, topPadding: Dp = 0.dp) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        // 标题与上方分组卡片之间留出比下方（8dp）更大的间距，避免两个分组贴在一起。
        modifier = Modifier.padding(start = 16.dp, top = topPadding, bottom = 8.dp),
    )
}

/**
 * 未读跳转目标在「置顶会话 / 全部会话」两段分组后的 LazyColumn 实际 item 下标：
 * - 有置顶：0=置顶标题，1..p=置顶行，p+1=全部标题，p+2..=普通行；
 * - 无置顶：0=全部标题，1..=普通行；
 * - 找不到目标返回 -1。
 */
private fun groupedChatItemIndex(chats: List<ChatItem>, targetChatId: String): Int {
    val pinned = chats.filter { it.isPinned }
    val pinnedPosition = pinned.indexOfFirst { it.id == targetChatId }
    if (pinnedPosition >= 0) return 1 + pinnedPosition
    val normal = chats.filterNot { it.isPinned }
    val normalPosition = normal.indexOfFirst { it.id == targetChatId }
    if (normalPosition < 0) return -1
    return if (pinned.isNotEmpty()) {
        1 + pinned.size + 1 + normalPosition
    } else {
        1 + normalPosition
    }
}

/** 群助手固定图标（不使用普通头像/在线点）。 */
@Composable
internal fun GroupAssistantAvatarMaterial(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** 单条会话行：48dp 渐变头像 + 名称/摘要 + 时间/未读/免打扰/置顶标签。 */
@Composable
internal fun ChatListRowMaterial(
    chat: ChatItem,
    actions: ChatActions,
    scrolling: Boolean = false,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    // 摘要前可能带提醒标签（如 [有人@我]）：标签用主题红色，其余保持 secondary 文字色。
    val summary = chatSummaryAnnotated(chat, tagColor = colorScheme.error)

    ChatCardActionMenuHost(chat = chat, actions = actions) {
        SegmentedListItem(
            // 置顶会话不套用「选中」样式：与普通会话一致的 segmented 卡片（仅保留置顶标签/排序区分）。
            selected = false,
            onClick = onClick,
            leadingContent = {
                if (chat.id == GroupAssistantChatId) {
                    GroupAssistantAvatarMaterial()
                } else if (chat.dataLineKind != 0) {
                    DataLineAvatar(kind = chat.dataLineKind)
                } else {
                    ChatAvatar(
                        name = chat.name,
                        seed = chat.avatarSeed,
                        isOnline = chat.isOnline,
                        isGroup = chat.isGroup,
                        avatarPath = chat.avatarPath,
                    )
                }
            },
            headlineContent = {
                Text(
                    text = chat.name,
                    // 滚动中暂停跑马灯（basicMarquee 会带来持续 layout/动画开销）；停止滚动后自动恢复。
                    modifier = if (scrolling) Modifier else Modifier.basicMarquee(),
                    fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    text = summary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = chat.timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (chat.isPinned) {
                            StatusTag(
                                label = stringResource(R.string.chat_pinned),
                                backgroundColor = colorScheme.secondaryContainer,
                                contentColor = colorScheme.onSecondaryContainer,
                            )
                        }
                        if (chat.isMuted) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsOff,
                                contentDescription = stringResource(R.string.chat_muted),
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        if (chat.unreadCount > 0) {
                            // 免打扰会话的未读徽标用更淡的 surfaceVariant 角色，与正常未读（primary）区分。
                            val mutedUnread = chat.isMuted
                            Badge(
                                containerColor = if (mutedUnread) {
                                    colorScheme.surfaceVariant
                                } else {
                                    colorScheme.primary
                                },
                                contentColor = if (mutedUnread) {
                                    colorScheme.onSurfaceVariant
                                } else {
                                    colorScheme.onPrimary
                                },
                            ) {
                                Text(text = unreadBadgeText(chat.unreadCount))
                            }
                        }
                    }
                }
            },
        )
    }
}

/** 消息页右上角加号菜单项：创建群聊 / 加好友群 / 扫一扫（点击走 [ChatActions] 对应回调）。 */
private enum class HomePlusMenu(val labelRes: Int, val icon: ImageVector) {
    CreateGroup(R.string.home_plus_create_group, Icons.Filled.GroupAdd),
    AddFriendGroup(R.string.home_plus_add_friend, Icons.Filled.PersonAdd),
    Scan(R.string.home_plus_scan, Icons.Filled.QrCodeScanner),
}
